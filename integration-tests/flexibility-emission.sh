#!/bin/bash
set -e

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

echo "POSTing data to $FLEXIBILITY_EMISSION_URL/FlexibilityEmission/evaluate..."
curl -s -f -X POST "$FLEXIBILITY_EMISSION_URL/FlexibilityEmission/evaluate" \
    -H "Content-Type: application/json" \
    -d @evaluate_payload.json

TOPIC="flexibility-offers"
echo "Checking topic: $TOPIC"

../kafka-binary/bin/kafka-console-consumer.sh \
    --bootstrap-server "$KAFKA_CLUSTER" \
    --topic "$TOPIC" \
    --from-beginning \
    --timeout-ms 20000 \
    --max-messages 1 || echo "No messages found or timed out."

echo "-------------------------------------------------"
