#!/bin/bash
echo "Starting..."

sudo yum install -y docker

sudo service docker start


echo "Finished."
sudo docker login -u "joaoruss0" -p "kybkoh-namko7-kAdhep"
sudo docker pull joaoruss0/grid-balancing:1.0.0-SNAPSHOT
sudo docker run -d --name grid-balancing -p 8080:8080 joaoruss0/grid-balancing:1.0.0-SNAPSHOT
