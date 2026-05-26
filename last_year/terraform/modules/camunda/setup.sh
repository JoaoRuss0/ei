#!/bin/bash
until curl -s -o /dev/null -w "%{http_code}" http://$publicdns_camunda:8080/engine-rest/engine | grep -q "200"; do
  sleep 5
done
echo "Camunda is up and running!"

cd ../bpmn
echo "Deploying BPMN files..."

find . -type f -name "*.bpmn" | while read file; do
  echo "Deploying $file..."
  curl -X POST -H "Content-Type: multipart/form-data" \
       -F "deployment-name=$(basename "$file")" \
       -F "data=@$file" \
       http://$publicdns_camunda:8080/engine-rest/deployment/create
  if [ $? -eq 0 ]; then
    echo "$file deployed successfully."
  else
    echo "Failed to deploy $file."
  fi
done
echo "All BPMN files deployed."