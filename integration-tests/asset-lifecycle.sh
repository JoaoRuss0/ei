#!/bin/bash
set -e
set -u

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

BASE_URL="$ASSET_URL"
ENTITY="Asset"
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
echo "Asset lifecycle (CRUD) integration test"
echo "-------------------------------------------------"

echo "Step 1: GET /$ENTITY (read all)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-all expected 200, got $STATUS"; exit 1; }
INITIAL_COUNT=$(echo "$BODY" | jq 'length')
echo "[OK]   read-all -> $STATUS, $INITIAL_COUNT existing records"

echo "Step 2: POST /$ENTITY (create one)"
CREATE_PAYLOAD='{"name":"lifecycle-test-asset","prosumerId":1,"type":"BATTERY"}'
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
[ "$NAME" = "lifecycle-test-asset" ] || { echo "[FAIL] read-one name='$NAME', expected 'lifecycle-test-asset'"; exit 1; }
echo "[OK]   read-one -> $STATUS, name=$NAME"

echo "Step 4: PUT /$ENTITY/$CREATED_ID (update)"
UPDATE_PAYLOAD='{"name":"lifecycle-test-asset-updated","prosumerId":1,"type":"SOLAR"}'
assert_status 204 "update" -X PUT "$BASE_URL/$ENTITY/$CREATED_ID" \
    -H "Content-Type: application/json" \
    -d "$UPDATE_PAYLOAD"

echo "Step 5: GET /$ENTITY/$CREATED_ID (read one after update)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/$CREATED_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-one-after-update expected 200, got $STATUS"; exit 1; }
NAME=$(echo "$BODY" | jq -r '.name')
TYPE=$(echo "$BODY" | jq -r '.type')
[ "$NAME" = "lifecycle-test-asset-updated" ] || { echo "[FAIL] post-update name='$NAME'"; exit 1; }
[ "$TYPE" = "SOLAR" ] || { echo "[FAIL] post-update type='$TYPE'"; exit 1; }
echo "[OK]   read-one -> $STATUS, name=$NAME, type=$TYPE"

echo "Step 6: DELETE /$ENTITY/$CREATED_ID"
assert_status 204 "delete" -X DELETE "$BASE_URL/$ENTITY/$CREATED_ID"

echo "Step 7: GET /$ENTITY/$CREATED_ID (expect 404 after delete)"
assert_status 404 "read-one-after-delete" "$BASE_URL/$ENTITY/$CREATED_ID"

CREATED_ID=""   # already cleaned by step 6, skip trap delete
echo "-------------------------------------------------"
echo "Asset lifecycle complete."
echo "-------------------------------------------------"
echo ""
echo ""
