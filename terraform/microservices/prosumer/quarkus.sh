#!/bin/bash
echo "Starting..."
sudo yum install -y docker
sudo service docker start
echo "Finished."

sudo docker login -u "joaoruss0" -p "kybkoh-namko7-kAdhep"
sudo docker pull joaoruss0/prosumer:1.0.0-SNAPSHOT
sudo docker run -d --name prosumer -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="mysql://ieproject202620260510204619968800000001.chvkqwfiehtt.us-east-1.rds.amazonaws.com:3306/VPPaaS" -e KAFKA_BOOTSTRAP_SERVERS="ec2-3-87-58-103.compute-1.amazonaws.com:9092,ec2-44-203-76-195.compute-1.amazonaws.com:9092,ec2-44-211-64-192.compute-1.amazonaws.com:9092" joaoruss0/prosumer:1.0.0-SNAPSHOT
