#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/DiscountCoupon"

# Step 1: GET all coupons (initial check)
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "Initial GET all coupons: $response"
echo "$response" | grep -q '\[' || { echo "Test failed: expected JSON array from GET all"; exit 1; }

# Step 2: Create a discount coupon
post_data='{
  "idLoyaltyCard": 1,
  "idsShops": [1, 2, 3],
  "discount": 20,
  "expirationDate": "2025-12-31T23:59:59"
}'
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST coupon response: $response"

# Step 3: Extract the coupon ID from the POST response
coupon_id=$(echo "$response" | grep -oP '"id":\s*\K\d+')
if [ -z "$coupon_id" ]; then
  echo "Test failed: no coupon ID found in POST response"
  exit 1
fi
echo "Created coupon ID: $coupon_id"

# Step 4: GET all coupons after POST
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all coupons after POST: $response"
echo "$response" | grep -q "\"id\":$coupon_id" || { echo "Test failed: created coupon not found in GET all after POST"; exit 1; }

# Step 5: GET coupon by ID
response=$(curl -s -X GET "$API_URL/$coupon_id" -H 'accept: application/json')
echo "GET coupon by ID: $response"
echo "$response" | grep -q "\"id\":$coupon_id" || { echo "Test failed: coupon not found by ID"; exit 1; }

# Step 6: DELETE coupon by ID
response=$(curl -s -X DELETE "$API_URL/$coupon_id" -H 'accept: application/json')
echo "DELETE coupon response: $response"

# Step 7: GET all coupons after delete
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all coupons after delete: $response"
echo "$response" | grep -q "\"id\":$coupon_id" && { echo "Test failed: deleted coupon still present in GET all"; exit 1; }

echo "All tests passed successfully!"
