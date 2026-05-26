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

svc_meta() {
    case "$1" in
        Asset)                       echo "asset asset" ;;
        AssetLink)                   echo "asset_link assetlink" ;;
        EnergyAnalytics)             echo "energy_analytics energyanalytics" ;;
        FlexibilityEmission)         echo "flexibility_emission flexibilityemission" ;;
        GridBalancingRecommendation) echo "grid_balancing gridbalancingrecommendation" ;;
        GridCell)                    echo "grid_cell gridcell" ;;
        Prosumer)                    echo "prosumer prosumer" ;;
        Telemetry)                   echo "telemetry telemetry" ;;
        UtilityOperator)             echo "utility_operator utilityoperator" ;;
        *) echo "" ;;
    esac
}

mkdir -p logs

build_one() {
    local svc="$1"
    echo "[BUILD] $svc"
    (
        cd "microservices/$svc" || exit 1
        ./mvnw -B -q clean package -DskipTests \
            -Dquarkus.container-image.group="$DOCKER_USERNAME"
    )
    echo "[BUILT] $svc"
}

if [ "$DO_BUILD" = "1" ]; then
    if ! docker info >/dev/null 2>&1; then
        echo "[ERROR] Docker daemon not reachable. Start Docker Desktop (or the daemon) and retry." >&2
        exit 1
    fi
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin > /dev/null

    PIDS=()
    for svc in "${SERVICES[@]}"; do
        build_one "$svc" > "logs/build_$svc.log" 2>&1 &
        PIDS+=($!)
    done
    wait "${PIDS[@]}"
    echo "[ALL BUILDS COMPLETE]"
fi

refresh_one() {
    local svc="$1"
    local meta tf img dns image db_addr kafka
    meta=$(svc_meta "$svc")
    [ -z "$meta" ] && { echo "[skip] $svc: unknown service"; return 0; }
    tf=$(echo "$meta" | awk '{print $1}')
    img=$(echo "$meta" | awk '{print $2}')

    dns=$(cd "terraform/microservices/$tf" \
            && terraform state show "module.$img.aws_instance.quarkus_instance" 2>/dev/null \
            | awk -F\" '/^    public_dns/ {print $2}')
    if [ -z "$dns" ]; then
        echo "[skip] $svc: no live EC2 (terraform state empty for module.$img)"
        return 0
    fi

    image="$DOCKER_USERNAME/$img:1.0.0-SNAPSHOT"
    echo "[refresh] $svc -> $dns ($image)"

    ssh -i config/key2.pem \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=15 \
        -o LogLevel=ERROR \
        ec2-user@"$dns" \
        "DOCKER_USERNAME='$DOCKER_USERNAME' DOCKER_PASSWORD='$DOCKER_PASSWORD' IMAGE='$image' NAME='$img' DATA_SOURCE='mysql://$DB_ADDRESS:3306/VPPaaS' KAFKA_CLUSTER='$KAFKA_CLUSTER' bash -s" <<'REMOTE'
set -e
sudo docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD" >/dev/null 2>&1
sudo docker pull "$IMAGE" >/dev/null
sudo docker stop "$NAME" >/dev/null 2>&1 || true
sudo docker rm "$NAME" >/dev/null 2>&1 || true
sudo docker run -d --name "$NAME" -p 8080:8080 \
    -e QUARKUS_DATASOURCE_REACTIVE_URL="$DATA_SOURCE" \
    -e KAFKA_BOOTSTRAP_SERVERS="$KAFKA_CLUSTER" \
    "$IMAGE" >/dev/null
echo "  $(sudo docker ps --filter name=$NAME --format '{{.Image}} -> {{.Status}}')"
REMOTE
    echo "[refreshed] $svc"
}

if [ "$DO_REFRESH" = "1" ]; then
    source ./addresses.sh > /dev/null
    if [ -z "$DB_ADDRESS" ] || [ -z "$KAFKA_CLUSTER" ]; then
        echo "[ERROR] DB_ADDRESS or KAFKA_CLUSTER missing — is the infrastructure deployed?" >&2
        exit 1
    fi
    if [ ! -f config/key2.pem ]; then
        echo "[ERROR] config/key2.pem missing — needed to SSH into microservice EC2s." >&2
        exit 1
    fi
    chmod 600 config/key2.pem 2>/dev/null || true

    PIDS=()
    for svc in "${SERVICES[@]}"; do
        refresh_one "$svc" > "logs/refresh_$svc.log" 2>&1 &
        PIDS+=($!)
    done
    wait "${PIDS[@]}"
    echo "[ALL REFRESHES COMPLETE]"
fi
