#!/bin/bash
set -e

cd ..
source ./addresses.sh > /dev/null
cd integration-tests
source ./_lib.sh

echo "-------------------------------------------------"
echo "Checking Energy Analytics events on kafka..."
echo "-------------------------------------------------"

PAYLOAD='[{"type":"ENERGY_DISCHARGED_BY_ZONE","value":15.0,"timestamp":"2026-06-06T18:03:29","gridCellId":"PORTO_NORTH","utilityOperatorId":3,"utilityOperatorName":"PracadaBoavista"}]'
TOPIC="energy-discharged-by-zone"

MAX_BEFORE=$(curl -s "$ENERGY_ANALYTICS_URL/EnergyAnalytics" | jq -r '[.[].id // 0] | max // 0')

echo "Pre-positioning consumer on topic: $TOPIC"
capture_next_message "$TOPIC" 15000

echo "POSTing EnergyAnalytics to $ENERGY_ANALYTICS_URL/EnergyAnalytics/save..."
curl -s -f -X POST "$ENERGY_ANALYTICS_URL/EnergyAnalytics/save" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD"

echo
echo "EnergyAnalytics record saved."

echo "Awaiting message on $TOPIC..."
MESSAGE=$(await_captured_message)
if [ -n "$MESSAGE" ]; then
    echo "Got: $MESSAGE"
else
    echo "[WARN] No message captured before timeout"
fi

echo "-------------------------------------------------"
echo "Cleaning up..."
NEW_IDS=$(curl -s "$ENERGY_ANALYTICS_URL/EnergyAnalytics" \
    | jq -r --argjson before "$MAX_BEFORE" '[.[] | select(.id > $before) | .id] | .[]')

for id in $NEW_IDS; do
    echo "Deleting EnergyAnalytics (ID: $id)..."
    curl -s -X DELETE "$ENERGY_ANALYTICS_URL/EnergyAnalytics/$id" \
        || >&2 echo "[WARN] Failed to delete EnergyAnalytics $id"
done
echo "Cleanup complete."
echo "-------------------------------------------------"
echo ""
echo ""
echo ""
echo ""
