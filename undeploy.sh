#!/bin/bash

source ./access.sh
mkdir -p logs

destroy() {
    local name="$1"
    local dir="$2"
    (
        cd "$dir" || { echo "Failed to cd to $dir"; exit 1; }
        terraform destroy -auto-approve -no-color
    ) > "logs/${name}_destroy.log" 2>&1
}

destroy telemetry        Quarkus-Terraform/telemetry        & TEL_PID=$!
destroy prosumer         Quarkus-Terraform/prosumer         & PRO_PID=$!
destroy utilityoperator  Quarkus-Terraform/utilityoperator  & UTO_PID=$!
destroy assetlink        Quarkus-Terraform/assetlink        & ASL_PID=$!
#destroy rds              RDS-Terraform                      & RDS_PID=$!
#destroy kafka            Kafka                              & KAF_PID=$!

wait $TEL_PID $PRO_PID $UTO_PID $ASL_PID $RDS_PID $KAF_PID