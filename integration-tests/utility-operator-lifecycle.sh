#!/bin/bash
set -e
set -u

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

BASE_URL="$UTILITY_OPERATOR_URL"
ENTITY="UtilityOperator"
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
echo "UtilityOperator lifecycle (CRUD) integration test"
echo "-------------------------------------------------"

echo "Step 1: GET /$ENTITY (read all)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-all expected 200, got $STATUS"; exit 1; }
INITIAL_COUNT=$(echo "$BODY" | jq 'length')
echo "[OK]   read-all -> $STATUS, $INITIAL_COUNT existing records"

echo "Step 2: POST /$ENTITY (create one)"
CREATE_PAYLOAD='{"name":"LifecycleTestOperator","location":"TestLand","iban":"PT50999999999999999999999"}'
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/$ENTITY" \
    -H "Content-Type: application/json" \
    -d "$CREATE_PAYLOAD")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "201" ] || { echo "[FAIL] create expected 201, got $STATUS"; exit 1; }
CREATED_ID=$(echo "$BODY" | jq -r '.id')
echo "[OK]   create -> $STATUS, id=$CREATED_ID"

echo "Step 3: GET /$ENTITY/$CREATED_ID (read one)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/$CREATED_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-one expected 200, got $STATUS"; exit 1; }
NAME=$(echo "$BODY" | jq -r '.name')
LOCATION=$(echo "$BODY" | jq -r '.location')
IBAN=$(echo "$BODY" | jq -r '.iban')
[ "$NAME" = "LifecycleTestOperator" ] || { echo "[FAIL] read-one name='$NAME'"; exit 1; }
[ "$LOCATION" = "TestLand" ] || { echo "[FAIL] read-one location='$LOCATION'"; exit 1; }
[ "$IBAN" = "PT50999999999999999999999" ] || { echo "[FAIL] read-one iban='$IBAN'"; exit 1; }
echo "[OK]   read-one -> $STATUS, name=$NAME, location=$LOCATION"

echo "Step 4: PUT /$ENTITY/$CREATED_ID (update)"
UPDATE_PAYLOAD='{"name":"LifecycleTestOperatorUpdated","location":"TestLandUpdated","iban":"PT50999999999999999999998"}'
assert_status 204 "update" -X PUT "$BASE_URL/$ENTITY/$CREATED_ID" \
    -H "Content-Type: application/json" \
    -d "$UPDATE_PAYLOAD"

echo "Step 5: GET /$ENTITY/$CREATED_ID (read one after update)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/$CREATED_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-one-after-update expected 200, got $STATUS"; exit 1; }
NAME=$(echo "$BODY" | jq -r '.name')
LOCATION=$(echo "$BODY" | jq -r '.location')
IBAN=$(echo "$BODY" | jq -r '.iban')
[ "$NAME" = "LifecycleTestOperatorUpdated" ] || { echo "[FAIL] post-update name='$NAME'"; exit 1; }
[ "$LOCATION" = "TestLandUpdated" ] || { echo "[FAIL] post-update location='$LOCATION'"; exit 1; }
[ "$IBAN" = "PT50999999999999999999998" ] || { echo "[FAIL] post-update iban='$IBAN'"; exit 1; }
echo "[OK]   read-one -> $STATUS, name=$NAME, location=$LOCATION"

echo "Step 6: DELETE /$ENTITY/$CREATED_ID"
assert_status 204 "delete" -X DELETE "$BASE_URL/$ENTITY/$CREATED_ID"

echo "Step 7: GET /$ENTITY/$CREATED_ID (expect 404 after delete)"
assert_status 404 "read-one-after-delete" "$BASE_URL/$ENTITY/$CREATED_ID"

CREATED_ID=""   # already cleaned by step 6, skip trap delete
echo "-------------------------------------------------"
echo "UtilityOperator lifecycle complete."
echo "-------------------------------------------------"
echo ""
echo ""
