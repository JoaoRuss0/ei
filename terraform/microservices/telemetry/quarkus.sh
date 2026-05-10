#!/bin/bash
echo "Starting..."

sudo yum install -y docker

sudo service docker start




echo "Finished."
sudo docker login -u "antoniomiguelpalma9999" -p "B@nanas.."
sudo docker pull antoniomiguelpalma9999/telemetry:1.0.0-SNAPSHOT
sudo docker run -d --name telemetry -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="mysql://ieproject202620260510145722655600000001.ccxcs7cjlufc.us-east-1.rds.amazonaws.com:3306/VPPaaS" -e KAFKA_BOOTSTRAP_SERVERS="ec2-18-212-26-42.compute-1.amazonaws.com:9092,ec2-3-84-35-99.compute-1.amazonaws.com:9092,ec2-98-93-174-175.compute-1.amazonaws.com:9092" antoniomiguelpalma9999/telemetry:1.0.0-SNAPSHOT
