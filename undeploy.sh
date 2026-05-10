#!/bin/bash

source ./access.sh
mkdir -p logs

destroy() {
    local name="$1"
    local dir="terraform/$2"
    (
        cd "$dir" || { echo "Failed to cd to $dir"; exit 1; }
        terraform destroy -auto-approve -no-color
    ) > "logs/${name}_destroy.log" 2>&1
}

echo "[KILLING ALL SERVICES] ..."

destroy asset                microservices/asset                      & ASS_PID=$!
destroy asset_link           microservices/asset_link                 & ASL_PID=$!
destroy energy_analytics     microservices/energy_analytics           & ENA_PID=$!
destroy flexibility_emission microservices/flexibility_emission       & FLX_PID=$!
destroy grid_balancing       microservices/grid_balancing             & GDB_PID=$!
destroy grid_cell            microservices/grid_cell                  & GDC_PID=$!
destroy kafka                kafka                                    & KAF_PID=$!
destroy prosumer             microservices/prosumer                   & PRO_PID=$!
destroy rds                  rds                                      & RDS_PID=$!
destroy telemetry            microservices/telemetry                  & TEL_PID=$!
destroy utility_operator     microservices/utility_operator           & UTO_PID=$!
destroy ollama               microservices/ollama                     & OLL_PID=$!

wait $ASS_PID $GDC_PID $TEL_PID $PRO_PID $UTO_PID $ASL_PID $RDS_PID $KAF_PID $ENA_PID $FLX_PID $GDB_PID $OLL_PID

echo "[KILLED]"