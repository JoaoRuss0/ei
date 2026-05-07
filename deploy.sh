#!/bin/bash

source ./access.sh

compile() {
    sed -i '' "/quarkus.datasource.reactive.url/d" application.properties
    sed -i '' "/quarkus.container-image.group/d" application.properties
    echo "quarkus.container-image.group=$DockerUsername" >> application.properties                                        
    echo "quarkus.datasource.reactive.url=mysql://$addressDB:3306/VPPaaS" >> application.properties                                        
    cd ../../..

    DockerImage="$(grep -m 1 "<artifactId>" pom.xml|sed "s/<artifactId>//g"|sed "s/<\/artifactId>//g" |sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g")"
    DockerImageVersion="$(grep -m 1 "<version>" pom.xml|sed "s/<version>//g"|sed "s/<\/version>//g" |sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g")"

    ./mvnw clean package
}

deploy_microservice() {
    sed -i '' "/sudo docker login/d" quarkus.sh
    sed -i '' "/sudo docker pull/d" quarkus.sh
    sed -i '' "/sudo docker run/d" quarkus.sh

    echo "sudo docker login -u \"$DockerUsername\" -p \"$DockerPassword\"" >> quarkus.sh
    echo "sudo docker pull $DockerUsername/$DockerImage:$DockerImageVersion" >> quarkus.sh
    echo "sudo docker run -d --name $DockerImage -p 8080:8080 $DockerUsername/$DockerImage:$DockerImageVersion" >> quarkus.sh

    terraform init
    terraform taint aws_instance.exampleDeployQuarkus
    terraform apply -auto-approve
}

deploy_rds() {
  cd RDS-Terraform
  terraform init && terraform apply -auto-approve
  esc=$'\e'
  addressDB="$(terraform state show aws_db_instance.example |grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"

  echo "RDS IS AVAILABLE HERE:"
  terraform state show aws_db_instance.example |grep address
  terraform state show aws_db_instance.example |grep port
}

deploy_kafka() {
  cd Kafka
  terraform init && terraform apply -auto-approve
  esc=$'\e'
  addresskafka="$(terraform state show 'aws_instance.exampleKafkaConfiguration[0]'|grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"

  echo "KAFKA IS AVAILABLE HERE:"
  echo $addresskafka
}

deploy_telemetry() {
    cd microservices/Telemetry/src/main/resources
    sed -i '' "/kafka.bootstrap.servers/d" application.properties
    echo "kafka.bootstrap.servers=$addresskafka:9092" >> application.properties
    compile
    cd ../..

    cd Quarkus-Terraform/telemetry
    deploy_microservice

    echo "MICROSERVICE telemetry IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_asset_link() {
    cd microservices/AssetLink/src/main/resources
    compile
    cd ../..

    cd Quarkus-Terraform/assetlink
    deploy_microservice

    echo "MICROSERVICE assetlink IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_prosumer() {
    cd microservices/Prosumer/src/main/resources
    compile
    cd ../..

    cd Quarkus-Terraform/prosumer
    deploy_microservice

    echo "MICROSERVICE prosumer IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_utility_operator() {
    cd microservices/UtilityOperator/src/main/resources
    compile
    cd ../..

    cd Quarkus-Terraform/utilityoperator
    deploy_microservice

    echo "MICROSERVICE utilityoperator IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_flexibility_emission() {
    cd microservices/FlexibilityEmission/src/main/resources

    sed -i '' "/kafka.bootstrap.servers/d" application.properties
    sed -i '' "/quarkus.rest-client.telemetry-service.url/d" application.properties
    echo "kafka.bootstrap.servers=$addresskafka:9092" >> application.properties
    echo "quarkus.rest-client.telemetry-service.url=http://$addressTelemetry:8080" >> application.properties

    compile
    cd ../..

    cd Quarkus-Terraform/flexibility-emission
    deploy_microservice

    echo "MICROSERVICE flexibility emission IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_grid_balancing() {
    cd microservices/GridBalancing/src/main/resources

    sed -i '' "/kafka.bootstrap.servers/d" application.properties
    sed -i '' "/quarkus.rest-client.telemetry-service.url/d" application.properties
    echo "kafka.bootstrap.servers=$addresskafka:9092" >> application.properties
    echo "quarkus.rest-client.telemetry-service.url=http://$addressTelemetry:8080" >> application.properties

    compile
    cd ../..

    cd Quarkus-Terraform/grid-balancing
    deploy_microservice

    echo "MICROSERVICE grid balancing IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

deploy_energy_analytics() {
    cd microservices/EnergyAnalytics/src/main/resources

    sed -i '' "/kafka.bootstrap.servers/d" application.properties
    sed -i '' "/quarkus.rest-client.telemetry-service.url/d" application.properties
    echo "kafka.bootstrap.servers=$addresskafka:9092" >> application.properties
    echo "quarkus.rest-client.telemetry-service.url=http://$addressTelemetry:8080" >> application.properties

    compile
    cd ../..

    cd Quarkus-Terraform/energy-analytics
    deploy_microservice

    echo "MICROSERVICE grid balancing IS AVAILABLE HERE:"
    addressMS="$(terraform state show aws_instance.exampleDeployQuarkus |grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
    echo "http://""$addressMS"":8080/q/swagger-ui/"
}

mkdir -p logs

#deploy_rds > logs/rds.log 2>&1 & RDS_PID=$!
#deploy_kafka > logs/kafka.log 2>&1 & KAFKA_PID=$!
#wait $RDS_PID $KAFKA_PID

esc=$'\e'
export addressDB="$(cd RDS-Terraform && terraform state show aws_db_instance.example |grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"
export addresskafka="$(cd Kafka && terraform state show 'aws_instance.exampleKafkaConfiguration[0]'|grep public_dns | sed "s/public_dns//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/$esc\[[0-9;]*m//g" )"

#deploy_telemetry > logs/telemetry.log 2>&1 & TEL_PID=$!
#deploy_asset_link > logs/assetLink.log 2>&1 & ASL_PID=$!
#deploy_prosumer > logs/prosumer.log 2>&1 & PRO_PID=$!
#deploy_utility_operator > logs/utility_operator.log 2>&1 & UTO_PID=$!
deploy_flexibility_emission > logs/flexibility_emission.log 2>&1 & FXE_PID=$!
deploy_grid_balancing > logs/grid_balancing.log 2>&1 & GRB_PID=$!
deploy_energy_analytics > logs/energy_analytics.log 2>&1 & ENA_PID=$!
wait $TEL_PID $ASL_PID $PRO_PID $UTO_PID $FXE_PID $GRB_PID $ENA_PID