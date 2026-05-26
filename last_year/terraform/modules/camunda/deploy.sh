#!/bin/bash
echo "Starting..."
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo docker pull camunda/camunda-bpm-platform:latest
sudo docker run -d --name camunda -p 8080:8080 -e KONG_ADDRESS="${kong_address}" -e JAVA_OPTS="-DKONG_ADDRESS=${kong_address}" camunda/camunda-bpm-platform:latest
echo "Finished."