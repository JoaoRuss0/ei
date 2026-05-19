#!/bin/bash
echo "Starting..."

sudo yum install -y docker

sudo service docker start


echo "Finished."
sudo docker login -u "antoniomiguelpalma9999" -p "B@nanas.."
sudo docker pull antoniomiguelpalma9999/gridbalancingrecommendation:1.0.0-SNAPSHOT
sudo docker run -d --name gridbalancingrecommendation -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="mysql://:3306/VPPaaS" -e KAFKA_BOOTSTRAP_SERVERS="" antoniomiguelpalma9999/gridbalancingrecommendation:1.0.0-SNAPSHOT
