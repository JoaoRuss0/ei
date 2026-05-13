#!/bin/bash
echo "Starting..."
sudo yum install -y docker
sudo service docker start
echo "Finished."

IMAGE="${DOCKER_USERNAME}/${SERVICE_NAME}:1.0.0-SNAPSHOT"
sudo docker login -u "${DOCKER_USERNAME}" -p "${DOCKER_PASSWORD}"
sudo docker pull "$IMAGE"
sudo docker run -d --name "${SERVICE_NAME}" -p 8080:8080 -e QUARKUS_DATASOURCE_REACTIVE_URL="${DATA_SOURCE}" -e KAFKA_BOOTSTRAP_SERVERS="${KAFKA_CLUSTER}" "$IMAGE"
