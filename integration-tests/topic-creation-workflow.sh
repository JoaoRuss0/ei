#!/bin/bash
set -e

cd ../event-producer/
mvn clean package
cd ../integration-tests

cd ..
source addresses.sh > /dev/null
cd integration-tests

create_entity() {
    local url=$1
    local payload=$2
    local entity_name=$3

    local response=$(curl -s -i -X POST "$url" -H "Content-Type: application/json" -d "$payload")
    local id=$(echo "$response" | grep -i '^location:' | awk -F'/' '{print $NF}' | tr -d '\r' | tr -d ' ')

    if [ -z "$id" ]; then
            >&2 echo "[ERROR] Failed to create $entity_name. Response:"
            >&2 echo "$response"
            exit 1
        fi

    >&2 echo "Created $entity_name (ID: $id)"
    echo "$id"
}

UO_NAME="EDP"
UO_PAYLOAD='{"name": "'$UO_NAME'", "location": "Lisbon", "iban": "PT50123456789012345678901"}'
UO_ID=$(create_entity "$UTILITY_OPERATOR_URL/UtilityOperator" "$UO_PAYLOAD" "Utility Operator")

PRO_PAYLOAD='{"name": "Joao Russo", "location": "Porto", "FiscalNumber": 123456789}'
PRO_ID=$(create_entity "$PROSUMER_URL/Prosumer" "$PRO_PAYLOAD" "Prosumer")

GC_ID_STRING="ZONE-PORTO-345"
GC_PAYLOAD='{"id": "'$GC_ID_STRING'", "address": "Porto Center", "postalCode": "4000-000", "maxLoad": 5000, "operatorId": '$UO_ID', "xCoords": 23, "yCoords": -54, "peakHoursStartTime": "2026-05-10T18:00:00", "peakHoursEndTime": "2026-05-10T22:00:00"}'
GC_ID=$(create_entity "$GRID_CELL_URL/GridCell" "$GC_PAYLOAD" "Grid Cell")

ASSET_PAYLOAD='{"name": "Home Battery", "prosumerId": '$PRO_ID', "gridCellId": "'$GC_ID'", "type": "BATTERY"}'
ASSET_ID=$(create_entity "$ASSET_URL/Asset" "$ASSET_PAYLOAD" "Asset")

LINK_PAYLOAD='{"idProsumer": '$PRO_ID', "idUtilityOperator": '$UO_ID'}'
LINK_ID=$(create_entity "$ASSET_LINK_URL/AssetLink" "$LINK_PAYLOAD" "Asset Link")

>&2 echo "Requesting topic creation via AssetLink..."
curl -s -X POST "$ASSET_LINK_URL/AssetLink/topic/$LINK_ID/$UO_NAME"
>&2 echo "Topic creation triggered."

TOPIC_NAME="$LINK_ID-$UO_NAME"
>&2 echo "Provisioning consumer for topic: $TOPIC_NAME..."
curl -s -X POST "$TELEMETRY_URL/Telemetry/consume" -H "Content-Type: application/json" -d '{"topicName": "'$TOPIC_NAME'"}'
>&2 echo "Consumer listening."

java -jar ../event-producer/target/event-producer.jar \
    --broker-list "$KAFKA_CLUSTER" \
    --throughput 20 \
    --topic "$TOPIC_NAME" \
    --asset-id "$ASSET_ID" \
    --grid-cell-id "$GC_ID" &

PRODUCER_PID=$!
echo "Java Event Producer started in background (PID: $PRODUCER_PID)"

echo "Sleeping for 10 seconds..."
sleep 10

curl -s "$TELEMETRY_URL/Telemetry" | jq .
echo "Kafka events successfully consumed"

echo "Killing event-producer..."
kill $PRODUCER_PID

echo "-------------------------------------------------"
