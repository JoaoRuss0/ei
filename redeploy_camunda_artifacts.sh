#!/bin/bash

esc=$'\e'

if [[ -z "$addressCamunda" ]]; then
    addressCamunda="$(cd terraform/camunda && terraform state show aws_instance.camunda_engine_instance \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/$esc\[[0-9;]*m//g")"
fi

if [[ -z "$addressCamunda" ]]; then
    echo "ERROR: could not resolve Camunda address. Set addressCamunda or run from project root with terraform state available."
    exit 1
fi

echo "[CAMUNDA] http://${addressCamunda}:8080"

echo "[WAITING FOR CAMUNDA TO BE READY] ..."
for i in $(seq 1 60); do
    if curl -s -f -o /dev/null -m 5 "http://${addressCamunda}:8080/v2/topology"; then
        echo "Camunda is ready."
        break
    fi
    sleep 5
done

echo "[DEPLOYING CAMUNDA FORMS] ..."
while IFS= read -r entry; do
    echo "  - $entry"
    curl -s -L -X POST "http://${addressCamunda}:8080/v2/deployments" \
        -H "Accept: application/json" \
        -F "resources=@${entry}" > /dev/null
done < <(find ./bpmn -type f -name "*.form" ! -path "*/Generic-BPMN-Patterns-For-Your-Reuse/*" | sort)

echo "[DEPLOYING CAMUNDA DMN DECISIONS] ..."
while IFS= read -r entry; do
    echo "  - $entry"
    curl -s -L -X POST "http://${addressCamunda}:8080/v2/deployments" \
        -H "Accept: application/json" \
        -F "resources=@${entry}" > /dev/null
done < <(find ./bpmn -type f -name "*.dmn" ! -path "*/Generic-BPMN-Patterns-For-Your-Reuse/*" | sort)

echo "[DEPLOYING CAMUNDA BPMN PROCESSES] ..."
while IFS= read -r entry; do
    echo "  - $entry"
    curl -s -L -X POST "http://${addressCamunda}:8080/v2/deployments" \
        -H "Accept: application/json" \
        -F "resources=@${entry}" > /dev/null
done < <(find ./bpmn -type f -name "*.bpmn" ! -path "*/Generic-BPMN-Patterns-For-Your-Reuse/*" | sort)

echo "[CAMUNDA RESOURCES REDEPLOYED]"
