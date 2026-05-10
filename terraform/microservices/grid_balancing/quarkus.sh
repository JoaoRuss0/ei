#!/bin/bash
echo "Starting..."

sudo yum install -y docker

sudo service docker start


echo "Finished."
sudo docker login -u "joaoruss0" -p "kybkoh-namko7-kAdhep"
sudo docker pull joaoruss0/gridbalancingrecommendation:1.0.0-SNAPSHOT
sudo docker run -d --name gridbalancingrecommendation -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="mysql://ieproject202620260510034721990000000001.chvkqwfiehtt.us-east-1.rds.amazonaws.com:3306/VPPaaS" -e KAFKA_BOOTSTRAP_SERVERS="ec2-34-201-110-182.compute-1.amazonaws.com:9092,ec2-13-222-149-104.compute-1.amazonaws.com:9092,ec2-18-207-202-206.compute-1.amazonaws.com:9092" joaoruss0/gridbalancingrecommendation:1.0.0-SNAPSHOT
