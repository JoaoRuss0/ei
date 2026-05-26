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
(cd "terraform/kafka"                 || { echo "Failed to cd to terraform/kafka";                exit 1; } && terraform destroy -auto-approve -no-color) > "logs/kafka_destroy.log"   2>&1 & KAF_PID=$!
(cd "terraform/rds"                   || { echo "Failed to cd to terraform/rds";                  exit 1; } && terraform destroy -auto-approve -no-color) > "logs/rds_destroy.log"     2>&1 & RDS_PID=$!
(cd "terraform/microservices/ollama"  || { echo "Failed to cd to terraform/microservices/ollama"; exit 1; } && terraform destroy -auto-approve -no-color) > "logs/ollama_destroy.log"  2>&1 & OLL_PID=$!
(cd "terraform/camunda"               || { echo "Failed to cd to terraform/camunda";              exit 1; } && terraform destroy -auto-approve -no-color) > "logs/camunda_destroy.log" 2>&1 & CAM_PID=$!
(cd "terraform/kong"                  || { echo "Failed to cd to terraform/kong";                 exit 1; } && terraform destroy -auto-approve -no-color) > "logs/kong_destroy.log"    2>&1 & KON_PID=$!
(cd "terraform/konga"                 || { echo "Failed to cd to terraform/konga";                exit 1; } && terraform destroy -auto-approve -no-color) > "logs/konga_destroy.log"   2>&1 & KGA_PID=$!

wait $ASS_PID $GDC_PID $TEL_PID $PRO_PID $UTO_PID $ASL_PID $KAF_PID $ENA_PID $FLX_PID $GDB_PID $OLL_PID $RDS_PID $CAM_PID $KON_PID $KGA_PID

echo "[KILLED]"