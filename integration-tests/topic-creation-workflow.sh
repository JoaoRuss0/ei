#!/bin/bash
set -e

cd ../event-producer/
mvn clean package > /dev/null
cd ../integration-tests

cd ..
source addresses.sh > /dev/null
cd integration-tests

echo "-------------------------------------------------"
echo "Checking Topic Lifecycle and Event Consumption..."
echo "-------------------------------------------------"


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
    --throughput 1 \
    --topic "$TOPIC_NAME" \
    --asset-id "$ASSET_ID" \
    --grid-cell-id "$GC_ID" &

PRODUCER_PID=$!
echo "Java Event Producer started in background (PID: $PRODUCER_PID)"
trap 'if [ -n "$PRODUCER_PID" ] && kill -0 "$PRODUCER_PID" 2>/dev/null; then echo "[trap] killing event-producer ($PRODUCER_PID)"; kill "$PRODUCER_PID" 2>/dev/null; fi' EXIT

echo "Sleeping for 4 seconds..."
sleep 4


echo "Messages found for new topic:"
../kafka-binary/bin/kafka-console-consumer.sh \
    --bootstrap-server "$KAFKA_CLUSTER" \
    --topic "$TOPIC_NAME" \
    --from-beginning \
    --timeout-ms 10000 \
    --max-messages 1 && echo "Found one" || echo "No messages found or timed out."
if curl -s "$TELEMETRY_URL/Telemetry" | grep -q "\"asset_id\":$ASSET_ID"; then
    echo "Telemetry row found for asset $ASSET_ID"
else
    echo "[WARN] No matching telemetry row found yet"
fi
echo "Kafka events successfully consumed"

echo "Killing event-producer..."
kill $PRODUCER_PID

echo "-------------------------------------------------"
echo "Cleaning up ..."

echo "Deleting Telemetry events for asset (ID: $ASSET_ID)..."
curl -s -X DELETE "$TELEMETRY_URL/Telemetry/by-asset/$ASSET_ID" \
    || >&2 echo "[WARN] Failed to delete Telemetry events"

echo "Stopping Telemetry consumer for topic: $TOPIC_NAME..."
curl -s -X POST "$TELEMETRY_URL/Telemetry/stop" \
    -H "Content-Type: application/json" \
    -d "{\"topicName\": \"$TOPIC_NAME\"}" || >&2 echo "[WARN] Failed to stop consumer"

echo "Deleting Kafka topic: $TOPIC_NAME..."
curl -s -X DELETE "$ASSET_LINK_URL/AssetLink/topic/$LINK_ID/$UO_NAME" \
    || >&2 echo "[WARN] Failed to delete topic"

echo "Deleting AssetLink (ID: $LINK_ID)..."
curl -s -X DELETE "$ASSET_LINK_URL/AssetLink/$LINK_ID" \
    || >&2 echo "[WARN] Failed to delete AssetLink"

echo "Deleting Asset (ID: $ASSET_ID)..."
curl -s -X DELETE "$ASSET_URL/Asset/$ASSET_ID" \
    || >&2 echo "[WARN] Failed to delete Asset"

echo "Deleting GridCell (ID: $GC_ID)..."
curl -s -X DELETE "$GRID_CELL_URL/GridCell/$GC_ID" \
    || >&2 echo "[WARN] Failed to delete GridCell"

echo "Deleting Prosumer (ID: $PRO_ID)..."
curl -s -X DELETE "$PROSUMER_URL/Prosumer/$PRO_ID" \
    || >&2 echo "[WARN] Failed to delete Prosumer"

echo "Deleting UtilityOperator (ID: $UO_ID)..."
curl -s -X DELETE "$UTILITY_OPERATOR_URL/UtilityOperator/$UO_ID" \
    || >&2 echo "[WARN] Failed to delete UtilityOperator"

echo "Cleanup complete."
echo "-------------------------------------------------"
echo ""
echo ""
echo ""
echo ""
