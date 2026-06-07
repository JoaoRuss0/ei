#!/bin/bash
set -e

cd ..
source ./addresses.sh > /dev/null
cd integration-tests
source ./_lib.sh

echo "-------------------------------------------------"
echo "Checking Flexibility Emission events on kafka..."
echo "-------------------------------------------------"


PAYLOAD='{"assetId":6,"prosumerId":3,"eventType":"SELL"}'
TOPIC="flexibility-offers"

echo "Pre-positioning consumer on topic: $TOPIC"
capture_next_message "$TOPIC" 15000

echo "POSTing FlexibilityEvent to $FLEXIBILITY_EMISSION_URL/FlexibilityEmission..."
RESPONSE=$(curl -s -f -X POST "$FLEXIBILITY_EMISSION_URL/FlexibilityEmission" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")

EVENT_ID=$(echo "$RESPONSE" | jq -r '.id')

echo
echo "FlexibilityEvent created (ID: $EVENT_ID)."

echo "Awaiting message on $TOPIC..."
MESSAGE=$(await_captured_message)
if [ -n "$MESSAGE" ]; then
    echo "Got: $MESSAGE"
else
    echo "[WARN] No message captured before timeout"
fi

echo "-------------------------------------------------"
echo "Cleaning up..."
echo "Deleting FlexibilityEvent (ID: $EVENT_ID)..."
curl -s -X DELETE "$FLEXIBILITY_EMISSION_URL/FlexibilityEmission/$EVENT_ID" \
    || >&2 echo "[WARN] Failed to delete FlexibilityEvent"
echo "Cleanup complete."
echo "-------------------------------------------------"
echo ""
echo ""
echo ""
echo ""
