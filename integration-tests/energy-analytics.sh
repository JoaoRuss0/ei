#!/bin/bash
set -e

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

echo "POSTing data to $ENERGY_ANALYTICS_URL/EnergyAnalytics/analyse..."
curl -f -s -X POST "$ENERGY_ANALYTICS_URL/EnergyAnalytics/analyse" \
    -H "Content-Type: application/json" \
    -d @analytics_payload.json

echo "Data successfully analyzed."
TOPICS=(
    "energy-discharged-by-zone"
    "generated-energy-by-prosumer"
    "consumed-energy-by-prosumer"
    "average-soc"
)

consume_topic() {
    local topic=$1
    echo "Checking topic: $topic"

    ../kafka-binary/bin/kafka-console-consumer.sh \
        --bootstrap-server "$KAFKA_CLUSTER" \
        --topic "$topic" \
        --from-beginning \
        --timeout-ms 20000 \
        --max-messages 1 || echo "No messages found or timed out."

    echo "-------------------------------------------------"
}


for topic in "${TOPICS[@]}"; do
    consume_topic "$topic"
done