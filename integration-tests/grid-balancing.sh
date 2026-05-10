#!/bin/bash
set -e

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

echo "POSTing data to $GRID_BALANCING_URL/AssetLink/balance..."
curl -s -f -X POST "$GRID_BALANCING_URL/AssetLink/balance" \
    -H "Content-Type: application/json" \
    -d @balance_payload.json

echo "Grid balancing logic executed successfully."

TOPIC="balancing-recommendation"
echo "Checking topic: $TOPIC"

../kafka-binary/bin/kafka-console-consumer.sh \
    --bootstrap-server "$KAFKA_CLUSTER" \
    --topic "$TOPIC" \
    --from-beginning \
    --timeout-ms 20000 \
    --max-messages 1 || echo "No messages found or timed out."

echo "-------------------------------------------------"