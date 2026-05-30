#!/bin/bash
set -e
source ./config/docker_variables.sh

DO_BUILD=1
DO_REFRESH=1
SERVICES=()
for arg in "$@"; do
    case "$arg" in
        --no-refresh)   DO_REFRESH=0 ;;
        --refresh-only) DO_BUILD=0 ;;
        *)              SERVICES+=("$arg") ;;
    esac
done

ALL_SERVICES=(Asset AssetLink EnergyAnalytics FlexibilityEmission GridBalancingRecommendation GridCell Prosumer Telemetry UtilityOperator)
[ ${#SERVICES[@]} -eq 0 ] && SERVICES=("${ALL_SERVICES[@]}")

# Fields: <terraform-folder> <terraform-module> <docker-image> <ssh-key>
# module and image are the same for every service EXCEPT GridBalancing,
# whose terraform module drops the "recommendation" suffix.
svc_meta() {
    case "$1" in
        Asset)                       echo "asset asset asset key2.pem" ;;
        AssetLink)                   echo "asset_link assetlink assetlink key2.pem" ;;
        EnergyAnalytics)             echo "energy_analytics energyanalytics energyanalytics key.pem" ;;
        FlexibilityEmission)         echo "flexibility_emission flexibilityemission flexibilityemission key2.pem" ;;
        GridBalancingRecommendation) echo "grid_balancing gridbalancing gridbalancingrecommendation key.pem" ;;
        GridCell)                    echo "grid_cell gridcell gridcell key2.pem" ;;
        Prosumer)                    echo "prosumer prosumer prosumer key.pem" ;;
        Telemetry)                   echo "telemetry telemetry telemetry key2.pem" ;;
        UtilityOperator)             echo "utility_operator utilityoperator utilityoperator key.pem" ;;
        *) echo "" ;;
    esac
}

mkdir -p logs
: > logs/setup.log
for svc in "${SERVICES[@]}"; do
    : > "logs/$svc.log"
done

build_one() {
    local svc="$1"
    echo "[BUILD] $svc"
    cd "microservices/$svc" || exit 1
    # Dropped -q so the full Maven build output is captured in the service log.
    ./mvnw -B clean package -DskipTests \
        -Dquarkus.container-image.group="$DOCKER_USERNAME"
    echo "[BUILT] $svc"
}

if [ "$DO_BUILD" = "1" ]; then
    if ! docker info >> logs/setup.log 2>&1; then
        echo "[ERROR] Docker daemon not reachable. Start Docker Desktop (or the daemon) and retry." | tee -a logs/setup.log >&2
        exit 1
    fi
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin >> logs/setup.log 2>&1

    PIDS=()
    for svc in "${SERVICES[@]}"; do
        (build_one "$svc") >> "logs/$svc.log" 2>&1 &
        PIDS+=($!)
    done
    wait "${PIDS[@]}"
    echo "[ALL BUILDS COMPLETE]"
fi

refresh_one() {
    local svc="$1"
    local meta tf mod img key dns image
    meta=$(svc_meta "$svc")
    [ -z "$meta" ] && { echo "[skip] $svc: unknown service"; return 0; }
    tf=$(echo "$meta" | awk '{print $1}')
    mod=$(echo "$meta" | awk '{print $2}')
    img=$(echo "$meta" | awk '{print $3}')
    key=$(echo "$meta" | awk '{print $4}')

    # terraform stderr now flows to the service log (function fd 2 is the log)
    # instead of being discarded; empty output still yields a clean [skip].
    dns=$(cd "terraform/microservices/$tf" \
            && terraform state show "module.$mod.aws_instance.quarkus_instance" \
            | awk -F\" '/^    public_dns/ {print $2}')
    if [ -z "$dns" ]; then
        echo "[skip] $svc: no live EC2 (terraform state empty for module.$mod)"
        return 0
    fi

    image="$DOCKER_USERNAME/$img:1.0.0-SNAPSHOT"
    echo "[refresh] $svc -> $dns ($image) [key: $key]"

    ssh -i "config/$key" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=15 \
        -o LogLevel=ERROR \
        ec2-user@"$dns" \
        "DOCKER_USERNAME='$DOCKER_USERNAME' DOCKER_PASSWORD='$DOCKER_PASSWORD' IMAGE='$image' NAME='$img' DATA_SOURCE='mysql://$DB_ADDRESS:3306/VPPaaS' KAFKA_CLUSTER='$KAFKA_CLUSTER' bash -s" <<'REMOTE'
set -e
# All remote output is returned over SSH and captured in the service log.
sudo docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
sudo docker pull "$IMAGE"
sudo docker stop "$NAME" || true
sudo docker rm "$NAME" || true
sudo docker run -d --name "$NAME" -p 8080:8080 \
    -e QUARKUS_DATASOURCE_REACTIVE_URL="$DATA_SOURCE" \
    -e KAFKA_BOOTSTRAP_SERVERS="$KAFKA_CLUSTER" \
    "$IMAGE"
echo "  $(sudo docker ps --filter name=$NAME --format '{{.Image}} -> {{.Status}}')"
REMOTE
    echo "[refreshed] $svc"
}

if [ "$DO_REFRESH" = "1" ]; then
    echo "[FETCHING ADDRESSES]"
    ADDR_TMP=$(mktemp)
    source ./addresses.sh > "$ADDR_TMP" 2>&1
    cat "$ADDR_TMP" >> logs/setup.log
    for svc in "${SERVICES[@]}"; do
        cat "$ADDR_TMP" >> "logs/$svc.log"
    done
    rm -f "$ADDR_TMP"
    if [ -z "$DB_ADDRESS" ] || [ -z "$KAFKA_CLUSTER" ]; then
        echo "[ERROR] DB_ADDRESS or KAFKA_CLUSTER missing — is the infrastructure deployed?" | tee -a logs/setup.log >&2
        exit 1
    fi

    for svc in "${SERVICES[@]}"; do
        meta=$(svc_meta "$svc")
        [ -z "$meta" ] && continue
        k=$(echo "$meta" | awk '{print $4}')
        [ -z "$k" ] && continue
        if [ ! -f "config/$k" ]; then
            echo "[ERROR] config/$k missing — needed for $svc to SSH into its EC2." | tee -a logs/setup.log >&2
            exit 1
        fi
        chmod 600 "config/$k" 2>> logs/setup.log || true
    done

    PIDS=()
    for svc in "${SERVICES[@]}"; do
        refresh_one "$svc" >> "logs/$svc.log" 2>&1 &
        PIDS+=($!)
    done
    wait "${PIDS[@]}"
    echo "[ALL REFRESHES COMPLETE]"
fi