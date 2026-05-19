#!/bin/bash

esc=$'\e'
mkdir -p logs

destroy() {
    local name="$1"
    local dir="terraform/$2"
    (
        cd "$dir" || { echo "Failed to cd to $dir"; exit 1; }
        terraform destroy -auto-approve -no-color \
          -var="DATA_SOURCE=null" \
          -var="KAFKA_CLUSTER=null" \
          -var="DOCKER_USERNAME=null" \
          -var="DOCKER_PASSWORD=null"
    ) > "logs/${name}_destroy.log" 2>&1
}

cleanup() {
    echo -e "\n${esc}[31m[INTERRUPTING] Killing all background deployments...${esc}[0m"
    kill 0
}

trap cleanup SIGINT

echo "[KILLING ALL SERVICES] ..."

destroy asset                microservices/asset                      & ASS_PID=$!
destroy asset_link           microservices/asset_link                 & ASL_PID=$!
destroy energy_analytics     microservices/energy_analytics           & ENA_PID=$!
destroy flexibility_emission microservices/flexibility_emission       & FLX_PID=$!
destroy grid_balancing       microservices/grid_balancing             & GDB_PID=$!
destroy grid_cell            microservices/grid_cell                  & GDC_PID=$!
destroy prosumer             microservices/prosumer                   & PRO_PID=$!
destroy telemetry            microservices/telemetry                  & TEL_PID=$!
destroy utility_operator     microservices/utility_operator           & UTO_PID=$!
(cd "terraform/kafka"                 || { echo "Failed to cd to terraform/kafka";                exit 1; } && terraform destroy -auto-approve -no-color) > "logs/kafka_destroy.log"  2>&1 & KAF_PID=$!
(cd "terraform/rds"                   || { echo "Failed to cd to terraform/rds";                  exit 1; } && terraform destroy -auto-approve -no-color) > "logs/rds_destroy.log"    2>&1 & RDS_PID=$!
(cd "terraform/microservices/ollama"  || { echo "Failed to cd to terraform/microservices/ollama"; exit 1; } && terraform destroy -auto-approve -no-color) > "logs/ollama_destroy.log" 2>&1 & OLL_PID=$!

wait $ASS_PID $GDC_PID $TEL_PID $PRO_PID $UTO_PID $ASL_PID $KAF_PID $ENA_PID $FLX_PID $GDB_PID $OLL_PID $RDS_PID

echo "[KILLED]"