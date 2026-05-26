#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/Loyaltycard"

# Step 1: Initial GET all loyalty cards
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "Initial GET all loyalty cards: $response"
echo "$response" | grep -q '\[' || { echo "Test failed: expected JSON array from GET all"; exit 1; }

# Step 2: Create a new loyalty card
post_data='{
  "idCustomer": 1,
  "idShop": 1
}'
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST loyalty card response: $response"

# Step 3: Extract loyalty card ID from POST response
card_id=$(echo "$response" | grep -oP '"id"\s*:\s*"?\K\d+')
if [ -z "$card_id" ]; then
  echo "Test failed: no loyalty card ID found in POST response"
  exit 1
fi
echo "Created loyalty card ID: $card_id"

# Step 4: GET all loyalty cards after POST and confirm presence
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all loyalty cards after POST: $response"
echo "$response" | grep -q "\"id\":$card_id," || { echo "Test failed: created card not found in GET all after POST"; exit 1; }

# Step 5: GET loyalty card by ID
response=$(curl -s -X GET "$API_URL/$card_id" -H 'accept: application/json')
echo "GET loyalty card by ID: $response"
echo "$response" | grep -q "\"id\":$card_id," || { echo "Test failed: loyalty card not found by ID"; exit 1; }

# Step 6: GET loyalty card by Customer ID and Shop ID
response=$(curl -s -X GET "$API_URL/1/1" -H 'accept: application/json')
echo "GET loyalty card by Customer ID and Shop ID: $response"
echo "$response" | grep -q "\"id\":$card_id," || { echo "Test failed: loyalty card not found by Customer and Shop ID"; exit 1; }

# Step 7: DELETE loyalty card by Customer ID and Shop ID
response=$(curl -s -X DELETE "$API_URL/1/1" -H 'accept: application/json')
echo "DELETE loyalty card by Customer and Shop ID: $response"

# Step 8: Confirm card removed from GET all
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all loyalty cards after first delete: $response"
echo "$response" | grep -q "\"idCustomer\":1, \"idShop\":1" && { echo "Test failed: loyalty card not deleted (1/1)"; exit 1; }

# Step 9: POST another loyalty card
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST second loyalty card response: $response"

# Step 10: Extract new card ID
new_card_id=$(echo "$response" | grep -oP '"id"\s*:\s*"?\K\d+')
[ -z "$new_card_id" ] && { echo "Test failed: no new loyalty card ID found"; exit 1; }
echo "New loyalty card ID: $new_card_id"

# Step 11: DELETE by ID
response=$(curl -s -X DELETE "$API_URL/$new_card_id" -H 'accept: application/json')
echo "DELETE loyalty card by ID: $response"

# Step 12: Confirm deletion
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all loyalty cards after second delete: $response"
echo "$response" | grep -q "\"id\":$new_card_id," && { echo "Test failed: loyalty card not deleted by ID"; exit 1; }

echo "All tests passed successfully!"
