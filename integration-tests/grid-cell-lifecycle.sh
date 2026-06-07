#!/bin/bash
set -e
set -u

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

BASE_URL="$GRID_CELL_URL"
ENTITY="GridCell"
CREATED_ID=""

cleanup() {
    if [ -n "$CREATED_ID" ]; then
        echo "[cleanup] DELETE /$ENTITY/$CREATED_ID"
        curl -s -o /dev/null -X DELETE "$BASE_URL/$ENTITY/$CREATED_ID" || true
    fi
}
trap cleanup EXIT

assert_status() {
    local expected="$1"; shift
    local desc="$1"; shift
    local actual
    actual=$(curl -s -o /dev/null -w "%{http_code}" "$@")
    if [ "$actual" != "$expected" ]; then
        echo "[FAIL] $desc -> expected $expected, got $actual"
        exit 1
    fi
    echo "[OK]   $desc -> $actual"
}

echo "-------------------------------------------------"
echo "GridCell lifecycle (CRUD) integration test"
echo "-------------------------------------------------"

NEW_ID="LIFECYCLE_TEST_CELL"

echo "Step 1: GET /$ENTITY (read all)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-all expected 200, got $STATUS"; exit 1; }
INITIAL_COUNT=$(echo "$BODY" | jq 'length')
echo "[OK]   read-all -> $STATUS, $INITIAL_COUNT existing records"

echo "Step 2: POST /$ENTITY (create one, id=$NEW_ID)"
CREATE_PAYLOAD=$(cat <<EOF
{
  "id": "$NEW_ID",
  "address": "Rua dos Testes 1",
  "postalCode": "9999-999",
  "peakHoursStartTime": "2026-01-01T18:00:00",
  "peakHoursEndTime": "2026-01-01T21:00:00",
  "maxLoad": 100,
  "operatorId": 1,
  "xCoords": 99,
  "yCoords": 99
}
EOF
)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/$ENTITY" \
    -H "Content-Type: application/json" \
    -d "$CREATE_PAYLOAD")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "201" ] || { echo "[FAIL] create expected 201, got $STATUS"; exit 1; }
CREATED_ID=$(echo "$BODY" | jq -r '.id')
[ "$CREATED_ID" = "$NEW_ID" ] || { echo "[FAIL] create returned id='$CREATED_ID', expected '$NEW_ID'"; exit 1; }
echo "[OK]   create -> $STATUS, id=$CREATED_ID"

echo "Step 3: GET /$ENTITY/$CREATED_ID (read one)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/$CREATED_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-one expected 200, got $STATUS"; exit 1; }
ADDRESS=$(echo "$BODY" | jq -r '.address')
MAX_LOAD=$(echo "$BODY" | jq -r '.maxLoad')
[ "$ADDRESS" = "Rua dos Testes 1" ] || { echo "[FAIL] read-one address='$ADDRESS'"; exit 1; }
[ "$MAX_LOAD" = "100" ] || { echo "[FAIL] read-one maxLoad='$MAX_LOAD'"; exit 1; }
echo "[OK]   read-one -> $STATUS, address='$ADDRESS', maxLoad=$MAX_LOAD"

echo "Step 4: PUT /$ENTITY/$CREATED_ID (update)"
UPDATE_PAYLOAD=$(cat <<EOF
{
  "address": "Rua dos Testes 2 (Updated)",
  "postalCode": "9999-000",
  "peakHoursStartTime": "2026-01-01T17:00:00",
  "peakHoursEndTime": "2026-01-01T22:00:00",
  "maxLoad": 200,
  "operatorId": 1,
  "xCoords": 99,
  "yCoords": 99
}
EOF
)
assert_status 204 "update" -X PUT "$BASE_URL/$ENTITY/$CREATED_ID" \
    -H "Content-Type: application/json" \
    -d "$UPDATE_PAYLOAD"

echo "Step 5: GET /$ENTITY/$CREATED_ID (read one after update)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/$CREATED_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-one-after-update expected 200, got $STATUS"; exit 1; }
ADDRESS=$(echo "$BODY" | jq -r '.address')
MAX_LOAD=$(echo "$BODY" | jq -r '.maxLoad')
[ "$ADDRESS" = "Rua dos Testes 2 (Updated)" ] || { echo "[FAIL] post-update address='$ADDRESS'"; exit 1; }
[ "$MAX_LOAD" = "200" ] || { echo "[FAIL] post-update maxLoad='$MAX_LOAD'"; exit 1; }
echo "[OK]   read-one -> $STATUS, address='$ADDRESS', maxLoad=$MAX_LOAD"

echo "Step 6: DELETE /$ENTITY/$CREATED_ID"
assert_status 204 "delete" -X DELETE "$BASE_URL/$ENTITY/$CREATED_ID"

echo "Step 7: GET /$ENTITY/$CREATED_ID (expect 404 after delete)"
assert_status 404 "read-one-after-delete" "$BASE_URL/$ENTITY/$CREATED_ID"

CREATED_ID=""   # already cleaned by step 6, skip trap delete
echo "-------------------------------------------------"
echo "GridCell lifecycle complete."
echo "-------------------------------------------------"
echo ""
echo ""
