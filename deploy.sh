#!/bin/bash

source ./access.sh

get_terraform_dns() {
    terraform state show aws_instance.quarkus_instance \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/$esc\[[0-9;]*m//g"
}

update_container_image_group() {
    sed -i '' "/quarkus.container-image.group/d" application.properties
    echo "quarkus.container-image.group=$DockerUsername" >> application.properties
}

package() {
    DockerImage="$(grep -m 1 "<artifactId>" pom.xml|sed "s/<artifactId>//g"|sed "s/<\/artifactId>//g" |sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g")"
    DockerImageVersion="$(grep -m 1 "<version>" pom.xml|sed "s/<version>//g"|sed "s/<\/version>//g" |sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g")"

    ./mvnw clean package -DskipTests
}

build_and_terraform_init_service() {
    sed -i '' "/sudo docker login/d" quarkus.sh
    sed -i '' "/sudo docker pull/d" quarkus.sh
    sed -i '' "/sudo docker run/d" quarkus.sh

    echo "sudo docker login -u \"$DockerUsername\" -p \"$DockerPassword\"" >> quarkus.sh
    echo "sudo docker pull $DockerUsername/$DockerImage:$DockerImageVersion" >> quarkus.sh
    echo "sudo docker run -d --name $DockerImage -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL=\"mysql://$addressDB:3306/VPPaaS\" -e KAFKA_BOOTSTRAP_SERVERS=\"$addressKafka\" $DockerUsername/$DockerImage:$DockerImageVersion" >> quarkus.sh

    terraform init
    terraform taint aws_instance.quarkus_instance
    terraform apply -auto-approve
}

deploy_rds() {
  cd terraform/rds
  terraform init && terraform apply -auto-approve
  esc=$'\e'
  addressDB="$(terraform state show aws_db_instance.example |grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"

  echo "RDS IS AVAILABLE HERE:"
  terraform state show aws_db_instance.example |grep address
  terraform state show aws_db_instance.example |grep port
}

deploy_kafka() {
  cd terraform/kafka
  terraform init && terraform apply -auto-approve
  esc=$'\e'
  addressKafka="$(terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')"

  echo "KAFKA IS AVAILABLE HERE:"
  echo $addressKafka
}

deploy_ollama() {
    cd "terraform/microservices/ollama" || exit 1
    terraform init
    terraform taint aws_instance.ollama_instance
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

    cd "microservices/$service_name/src/main/resources" || exit 1
    update_container_image_group
    cd ../../.. || exit 1
    package
    cd ../.. || exit 1

    cd "terraform/microservices/$terraform_service_name" || exit 1
    build_and_terraform_init_service

    echo "MICROSERVICE $service_name IS AVAILABLE HERE:"
    addressMS="$(get_terraform_dns)"
    echo "http://$addressMS:8080/q/swagger-ui/"
}

mkdir -p logs

echo "[DEPLOYING RDS, OLLAMA AND THE KAFKA CLUSTER] ..."
deploy_kafka    > logs/kafka.log 2>&1       & KAF_PID=$!
deploy_rds      > logs/rds.log 2>&1         & RDS_PID=$!
deploy_ollama   > logs/ollama.log 2>&1      & OLL_PID=$!

wait $RDS_PID $KAF_PID $OLL_PID

echo "[DONE]"

esc=$'\e'
export addressDB="$(cd terraform/rds && terraform state show aws_db_instance.example | grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
export addressKafka="$(cd terraform/kafka && terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')"

echo "- Database:" "$addressDB"
echo "- Kafka Cluster:" "$addressKafka"
echo "- Ollama:" "$(cd terraform/microservices/ollama && terraform state show -no-color aws_instance.ollama_instance | awk -F\" '/^    public_dns/ {print $2}' )"

echo "[DEPLOYING ALL MICROSERVICES] ..."

(deploy_microservice "Asset"                        "asset"                         ) > logs/asset.log                 2>&1 & ASS_PID=$!
(deploy_microservice "GridCell"                     "grid_cell"                     ) > logs/grid_cell.log             2>&1 & GDC_PID=$!
(deploy_microservice "Prosumer"                     "prosumer"                      ) > logs/prosumer.log              2>&1 & PRO_PID=$!
(deploy_microservice "UtilityOperator"              "utility_operator"              ) > logs/utility_operator.log      2>&1 & UTO_PID=$!
(deploy_microservice "AssetLink"                    "asset_link"                    ) > logs/asset_link.log            2>&1 & ASL_PID=$!
(deploy_microservice "EnergyAnalytics"              "energy_analytics"              ) > logs/energy_analytics.log      2>&1 & ENA_PID=$!
(deploy_microservice "FlexibilityEmission"          "flexibility_emission"          ) > logs/flexibility_emission.log  2>&1 & FLX_PID=$!
(deploy_microservice "GridBalancingRecommendation"  "grid_balancing"                ) > logs/grid_balancing.log        2>&1 & GDB_PID=$!
(deploy_microservice "Telemetry"                    "telemetry"                     ) > logs/telemetry.log             2>&1 & TEL_PID=$!

wait $ASS_PID $GDC_PID $PRO_PID $UTO_PID $ASL_PID $ENA_PID $FLX_PID $GDB_PID $TEL_PID

echo "- Asset: http://""$(cd terraform/microservices/asset && get_terraform_dns)"":8080/q/swagger-ui"
echo "- GridCell: http://""$(cd terraform/microservices/grid_cell && get_terraform_dns)"":8080/q/swagger-ui"
echo "- Prosumer: http://""$(cd terraform/microservices/prosumer && get_terraform_dns)"":8080/q/swagger-ui"
echo "- UtilityOperator: http://""$(cd terraform/microservices/utility_operator && get_terraform_dns)"":8080/q/swagger-ui"
echo "- AssetLink: http://""$(cd terraform/microservices/asset_link && get_terraform_dns)"":8080/q/swagger-ui"
echo "- EnergyAnalytics: http://""$(cd terraform/microservices/energy_analytics && get_terraform_dns)"":8080/q/swagger-ui"
echo "- FlexibilityEmission: http://""$(cd terraform/microservices/flexibility_emission && get_terraform_dns)"":8080/q/swagger-ui"
echo "- GridBalancingRecommendation: http://""$(cd terraform/microservices/grid_balancing && get_terraform_dns)"":8080/q/swagger-ui"
echo "- Telemetry: http://""$(cd terraform/microservices/telemetry && get_terraform_dns)"":8080/q/swagger-ui"

echo "[DONE]"