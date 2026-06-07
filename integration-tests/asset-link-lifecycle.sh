#!/bin/bash
set -e
set -u

cd ..
source ./addresses.sh > /dev/null
cd integration-tests

BASE_URL="$ASSET_LINK_URL"
ENTITY="AssetLink"
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
echo "AssetLink lifecycle (CRD + lookups) integration test"
echo "AssetLink has no PUT endpoint; update step is replaced"
echo "by by-prosumer-id and by-utilityoperator-id checks."
echo "-------------------------------------------------"

# Pick IDs well outside the seed range to avoid the UC_Loyal UNIQUE
# constraint on (idProsumer, idUtilityOperator).
NEW_PROSUMER_ID=999000
NEW_OPERATOR_ID=999000

echo "Step 1: GET /$ENTITY (read all)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] read-all expected 200, got $STATUS"; exit 1; }
INITIAL_COUNT=$(echo "$BODY" | jq 'length')
echo "[OK]   read-all -> $STATUS, $INITIAL_COUNT existing records"

echo "Step 2: POST /$ENTITY (create one, idProsumer=$NEW_PROSUMER_ID, idUtilityOperator=$NEW_OPERATOR_ID)"
CREATE_PAYLOAD="{\"idProsumer\":$NEW_PROSUMER_ID,\"idUtilityOperator\":$NEW_OPERATOR_ID}"
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
PROSUMER=$(echo "$BODY" | jq -r '.idProsumer')
OPERATOR=$(echo "$BODY" | jq -r '.idUtilityOperator')
[ "$PROSUMER" = "$NEW_PROSUMER_ID" ] || { echo "[FAIL] read-one idProsumer='$PROSUMER'"; exit 1; }
[ "$OPERATOR" = "$NEW_OPERATOR_ID" ] || { echo "[FAIL] read-one idUtilityOperator='$OPERATOR'"; exit 1; }
echo "[OK]   read-one -> $STATUS, idProsumer=$PROSUMER, idUtilityOperator=$OPERATOR"

echo "Step 4: GET /$ENTITY/by-prosumer-id/$NEW_PROSUMER_ID (lookup by prosumer)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/by-prosumer-id/$NEW_PROSUMER_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] by-prosumer-id expected 200, got $STATUS"; exit 1; }
FOUND=$(echo "$BODY" | jq --argjson id "$CREATED_ID" '[.[] | select(.id == $id)] | length')
[ "$FOUND" = "1" ] || { echo "[FAIL] by-prosumer-id did not return created record (id=$CREATED_ID)"; exit 1; }
echo "[OK]   by-prosumer-id -> $STATUS, contains id=$CREATED_ID"

echo "Step 5: GET /$ENTITY/by-utilityoperator-id/$NEW_OPERATOR_ID (lookup by utility operator)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/$ENTITY/by-utilityoperator-id/$NEW_OPERATOR_ID")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
[ "$STATUS" = "200" ] || { echo "[FAIL] by-utilityoperator-id expected 200, got $STATUS"; exit 1; }
FOUND=$(echo "$BODY" | jq --argjson id "$CREATED_ID" '[.[] | select(.id == $id)] | length')
[ "$FOUND" = "1" ] || { echo "[FAIL] by-utilityoperator-id did not return created record (id=$CREATED_ID)"; exit 1; }
echo "[OK]   by-utilityoperator-id -> $STATUS, contains id=$CREATED_ID"

echo "Step 6: DELETE /$ENTITY/$CREATED_ID"
assert_status 204 "delete" -X DELETE "$BASE_URL/$ENTITY/$CREATED_ID"

echo "Step 7: GET /$ENTITY/$CREATED_ID (expect 404 after delete)"
assert_status 404 "read-one-after-delete" "$BASE_URL/$ENTITY/$CREATED_ID"

CREATED_ID=""   # already cleaned by step 6, skip trap delete
echo "-------------------------------------------------"
echo "AssetLink lifecycle complete."
echo "-------------------------------------------------"
echo ""
echo ""
