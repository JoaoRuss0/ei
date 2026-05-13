#!/bin/bash

esc=$'\e'
source ./config/docker_variables.sh

get_terraform_dns() {
    terraform state show "module.$1.aws_instance.quarkus_instance" \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/$esc\[[0-9;]*m//g"
}

deploy_rds() {
  cd terraform/rds
  terraform init && terraform apply -auto-approve
  addressDB="$(terraform state show aws_db_instance.example |grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"

  echo "RDS IS AVAILABLE HERE:"
  terraform state show aws_db_instance.example | grep address
  terraform state show aws_db_instance.example | grep port
}

deploy_kafka() {
  cd terraform/kafka
  terraform init && terraform apply -auto-approve
  addressKafka="$(terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')"

  echo "KAFKA IS AVAILABLE HERE:"
  echo $addressKafka
}

deploy_ollama() {
    cd "terraform/microservices/ollama" || exit 1
    terraform init
    terraform apply -auto-approve

    echo "MICROSERVICE ollama IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.ollama_instance \
                    | grep public_dns \
                    | sed 's/public_dns//g' \
                    | sed 's/=//g' \
                    | sed 's/"//g' \
                    | sed 's/ //g' \
                    | sed "s/$esc\[[0-9;]*m//g")"

    echo "http://$addressMS:11434/api/generate"
}

deploy_microservice() {
    local service_name="$1"
    local terraform_service_name="$2"

    cd "microservices/$service_name/" || exit 1
    ./mvnw clean package -DskipTests -Dquarkus.container-image.group="${DOCKER_USERNAME}"
    cd "../../terraform/microservices/$terraform_service_name/" || exit 1

    terraform init
    terraform taint "module.$service_name.aws_instance.quarkus_instance"
    terraform apply -auto-approve -var="DATA_SOURCE=mysql://${addressDB}:3306/VPPaaS" -var="KAFKA_CLUSTER=${addresskafka}" -var="DOCKER_USERNAME=${DOCKER_USERNAME}" -var="DOCKER_PASSWORD=${DOCKER_PASSWORD}"

    echo "MICROSERVICE $service_name IS AVAILABLE HERE:"
    addressMS="$(get_terraform_dns "$service_name")"
    echo "http://$addressMS:8080/q/swagger-ui/"
}

create_kafka_topics() {
    echo "[CREATING KAFKA TOPICS] ..."

    [[ ! -d "kafka-binary" ]] && curl -f -L -O https://dlcdn.apache.org/kafka/4.1.1/kafka_2.13-4.1.1.tgz && tar -xzf kafka_2.13-4.1.1.tgz && mv kafka_2.13-4.1.1 kafka-binary && rm kafka_2.13-4.1.1.tgz

    local topics=(
        "flexibility-offers"
        "balancing-recommendation"
        "energy-discharged-by-zone"
        "generated-energy-by-prosumer"
        "consumed-energy-by-prosumer"
        "average-soc"
    )

    for topic in "${topics[@]}"; do
        ./kafka-binary/bin/kafka-topics.sh --create \
            --if-not-exists \
            --bootstrap-server "$addressKafka" \
            --topic "$topic"
    done
    echo "[KAFKA TOPICS CREATED]"
}

cleanup() {
    echo -e "\n${esc}[31m[INTERRUPTING] Killing all background deployments...${esc}[0m"
    kill 0
}

trap cleanup SIGINT

mkdir -p logs

echo "[DEPLOYING RDS, OLLAMA AND THE KAFKA CLUSTER] ..."

deploy_kafka    > logs/kafka.log 2>&1       & KAF_PID=$!
deploy_rds      > logs/rds.log 2>&1         & RDS_PID=$!
deploy_ollama   > logs/ollama.log 2>&1      & OLL_PID=$!

wait $KAF_PID $OLL_PID $RDS_PID

echo "[DONE]"

export addressDB="$(cd terraform/rds && terraform state show aws_db_instance.example | grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
export addressKafka="$(cd terraform/kafka && terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')"

echo "- Database:" "$addressDB"
echo "- Kafka Cluster:" "$addressKafka"
echo "- Ollama: http://""$(cd terraform/microservices/ollama && terraform state show -no-color aws_instance.ollama_instance | awk -F\" '/^    public_dns/ {print $2}' )"":11434/api/generate"

echo "[DEPLOYING ALL MICROSERVICES] ..."

(deploy_microservice "Asset"                        "asset"                ) > logs/asset.log                 2>&1 & ASS_PID=$!
(deploy_microservice "GridCell"                     "grid_cell"            ) > logs/grid_cell.log             2>&1 & GDC_PID=$!
(deploy_microservice "Prosumer"                     "prosumer"             ) > logs/prosumer.log              2>&1 & PRO_PID=$!
(deploy_microservice "UtilityOperator"              "utility_operator"     ) > logs/utility_operator.log      2>&1 & UTO_PID=$!
(deploy_microservice "AssetLink"                    "asset_link"           ) > logs/asset_link.log            2>&1 & ASL_PID=$!
(deploy_microservice "EnergyAnalytics"              "energy_analytics"     ) > logs/energy_analytics.log      2>&1 & ENA_PID=$!
(deploy_microservice "FlexibilityEmission"          "flexibility_emission" ) > logs/flexibility_emission.log  2>&1 & FLX_PID=$!
(deploy_microservice "GridBalancingRecommendation"  "grid_balancing"       ) > logs/grid_balancing.log        2>&1 & GDB_PID=$!
(deploy_microservice "Telemetry"                    "telemetry"            ) > logs/telemetry.log             2>&1 & TEL_PID=$!

wait $ASS_PID $GDC_PID $PRO_PID $UTO_PID $ASL_PID $ENA_PID $FLX_PID $GDB_PID $TEL_PID

echo "- Asset: http://""$(cd terraform/microservices/asset                                && get_terraform_dns asset)"":8080/q/swagger-ui"
echo "- GridCell: http://""$(cd terraform/microservices/grid_cell                         && get_terraform_dns gridcell)"":8080/q/swagger-ui"
echo "- Prosumer: http://""$(cd terraform/microservices/prosumer                          && get_terraform_dns prosumer)"":8080/q/swagger-ui"
echo "- UtilityOperator: http://""$(cd terraform/microservices/utility_operator           && get_terraform_dns utilityoperator)"":8080/q/swagger-ui"
echo "- AssetLink: http://""$(cd terraform/microservices/asset_link                       && get_terraform_dns assetlink)"":8080/q/swagger-ui"
echo "- EnergyAnalytics: http://""$(cd terraform/microservices/energy_analytics           && get_terraform_dns energyanalytics)"":8080/q/swagger-ui"
echo "- FlexibilityEmission: http://""$(cd terraform/microservices/flexibility_emission   && get_terraform_dns flexibilityemission)"":8080/q/swagger-ui"
echo "- GridBalancingRecommendation: http://""$(cd terraform/microservices/grid_balancing && get_terraform_dns gridbalancing)"":8080/q/swagger-ui"
echo "- Telemetry: http://""$(cd terraform/microservices/telemetry                        && get_terraform_dns telemetry)"":8080/q/swagger-ui"

echo "[DONE]"

create_kafka_topics