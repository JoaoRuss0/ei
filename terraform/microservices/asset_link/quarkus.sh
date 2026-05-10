#!/bin/bash
echo "Starting..."

sudo yum install -y docker
sudo service docker start

echo "Finished."
sudo docker login -u "joaoruss0" -p "kybkoh-namko7-kAdhep"
sudo docker pull joaoruss0/assetlink:1.0.0-SNAPSHOT
sudo docker run -d --name assetlink -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="mysql://ieproject202620260509234312660200000001.chvkqwfiehtt.us-east-1.rds.amazonaws.com:3306/VPPaaS" -e KAFKA_BOOTSTRAP_SERVERS="ec2-98-94-49-148.compute-1.amazonaws.com:9092,ec2-54-175-188-195.compute-1.amazonaws.com:9092,ec2-3-95-161-248.compute-1.amazonaws.com:9092" joaoruss0/assetlink:1.0.0-SNAPSHOT
