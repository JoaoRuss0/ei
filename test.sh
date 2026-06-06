#!/bin/bash

SERVICES=(
  "Asset"
  "AssetLink"
  "EnergyAnalytics"
  "FlexibilityEmission"
  "GridBalancingRecommendation"
  "GridCell"
  "Prosumer"
  "Telemetry"
  "UtilityOperator"
)

echo "[STARTING TEST SUITE FOR ALL MICROSERVICES]..."

for service in "${SERVICES[@]}"; do
    echo ""
    echo "====================================================================="
    echo "Running './mvnw clean test' in: $service"
    echo "====================================================================="

    (cd "microservices/$service" && ./mvnw clean test)

    if [ $? -ne 0 ]; then
        echo ""
        echo "[ERROR] Tests failed in $service. Aborting ..."
        exit 1
    fi
done

echo ""
echo "----- DONE -----"
echo ""
echo ""
echo ""
echo ""
echo ""
echo ""
echo ""
echo "====================================================================="
echo "Running integration tests to check if events are sent/consumed"
echo "====================================================================="

cd integration-tests
./grid-balancing.sh
./topic-creation-workflow.sh
./ollama.sh

echo ""
echo "----- DONE -----"
