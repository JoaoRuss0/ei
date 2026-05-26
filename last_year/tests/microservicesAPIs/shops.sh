#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/Shop"

# Step 1: GET all shops (initial check)
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "Initial GET all shops: $response"
echo "$response" | grep -q '\[' || { echo "Test failed: expected JSON array from GET all"; exit 1; }

# Step 2: Create a new shop
post_data='{
  "address": "string",
  "postalCode": "string",
  "name": "string"
}'
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST shop response: $response"

# Step 3: Extract the shop ID from the POST response
shop_id=$(echo "$response" | grep -oP '"id":\s*\K\d+')
if [ -z "$shop_id" ]; then
  echo "Test failed: no shop ID found in POST response"
  exit 1
fi
echo "Created shop ID: $shop_id"

# Step 4: GET all shops after POST
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all shops after POST: $response"
echo "$response" | grep -q "\"id\":$shop_id" || { echo "Test failed: created shop not found in GET all after POST"; exit 1; }

# Step 5: GET shop by ID
response=$(curl -s -X GET "$API_URL/$shop_id" -H 'accept: application/json')
echo "GET shop by ID: $response"
echo "$response" | grep -q "\"id\":$shop_id" || { echo "Test failed: shop not found by ID"; exit 1; }

# Step 6: Update shop by ID (PUT)
put_data='{
  "address": "aaaa",
  "postalCode": "ppppp",
  "name": "nnnnn"
}'
response=$(curl -s -X PUT "$API_URL/$shop_id" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$put_data")
echo "PUT update response: $response"

# Step 7: GET shop by ID again to confirm update
response=$(curl -s -X GET "$API_URL/$shop_id" -H 'accept: application/json')
echo "GET shop by ID after update: $response"
echo "$response" | grep -q '"address":"aaaa"' || { echo "Test failed: update not confirmed (address)"; exit 1; }
echo "$response" | grep -q '"postalCode":"ppppp"' || { echo "Test failed: update not confirmed (postalCode)"; exit 1; }
echo "$response" | grep -q '"name":"nnnnn"' || { echo "Test failed: update not confirmed (name)"; exit 1; }

# Step 8: Delete shop by ID
response=$(curl -s -X DELETE "$API_URL/$shop_id" -H 'accept: application/json')
echo "DELETE shop response: $response"

# Step 9: GET all shops after delete (ensure shop is gone)
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all shops after delete: $response"
echo "$response" | grep -q "\"id\":$shop_id" && { echo "Test failed: deleted shop still present in GET all"; exit 1; }

echo "All tests passed successfully!"
