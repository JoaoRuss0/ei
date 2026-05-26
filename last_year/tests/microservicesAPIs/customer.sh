#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/Customer"

# Step 1: GET all customers (initial check)
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "Initial GET all customers: $response"
echo "$response" | grep -q '\[' || { echo "Test failed: expected JSON array from GET all"; exit 1; }

# Step 2: Create a new customer
post_data='{
  "FiscalNumber": 0,
  "address": "string",
  "postalCode": "string",
  "name": "string"
}'
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST customer response: $response"

# Step 3: Extract the customer ID from the POST response
cust_id=$(echo "$response" | grep -oP '"id":\s*\K\d+')
if [ -z "$cust_id" ]; then
  echo "Test failed: no customer ID found in POST response"
  exit 1
fi
echo "Created customer ID: $cust_id"

# Step 4: GET all customers after POST
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all customers after POST: $response"
echo "$response" | grep -q "\"id\":$cust_id" || { echo "Test failed: created customer not found in GET all after POST"; exit 1; }

# Step 5: GET customer by ID
response=$(curl -s -X GET "$API_URL/$cust_id" -H 'accept: application/json')
echo "GET customer by ID: $response"
echo "$response" | grep -q "\"id\":$cust_id" || { echo "Test failed: customer not found by ID"; exit 1; }

# Step 6: Update customer by ID (PUT)
put_data='{
  "FiscalNumber": 1,
  "address": "aaaaaa",
  "postalCode": "ppppp",
  "name": "nnnnn"
}'
response=$(curl -s -X PUT "$API_URL/$cust_id" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$put_data")
echo "PUT update response: $response"

# Step 7: GET customer by ID again to confirm update
response=$(curl -s -X GET "$API_URL/$cust_id" -H 'accept: application/json')
echo "GET customer by ID after update: $response"
echo "$response" | grep -q '"address":"aaaaaa"' || { echo "Test failed: update not confirmed (address)"; exit 1; }
echo "$response" | grep -q '"postalCode":"ppppp"' || { echo "Test failed: update not confirmed (postalCode)"; exit 1; }
echo "$response" | grep -q '"name":"nnnnn"' || { echo "Test failed: update not confirmed (name)"; exit 1; }
echo "$response" | grep -q '"FiscalNumber":1' || { echo "Test failed: update not confirmed (FiscalNumber)"; exit 1; }

# Step 8: Delete customer by ID
response=$(curl -s -X DELETE "$API_URL/$cust_id" -H 'accept: application/json')
echo "DELETE customer response: $response"

# Step 9: GET all customers after delete (ensure customer is gone)
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all customers after delete: $response"
echo "$response" | grep -q "\"id\":$cust_id" && { echo "Test failed: deleted customer still present in GET all"; exit 1; }

echo "All tests passed successfully!"

