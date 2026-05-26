#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/Purchase"

# Step 1: Check if the list is empty
response=$(curl -s -X GET "$API_URL" -H 'accept: application/json')
echo "GET all purchases: $response"
echo "$response" | grep -q '\[' || { echo "Test failed: expected JSON array from GET all"; exit 1; }

# Step 2: Make a POST request to /Purchase/Consume
post_data='{
  "TopicName": "1-2"
}'
response=$(curl -s -X POST "$API_URL/Consume" -H 'accept: text/plain' -H 'Content-Type: application/json' -d "$post_data")
echo "POST consume response: $response"
[ "$response" = "New worker started" ] || { echo "Test failed: expected response 'New worker started'"; exit 1; }

echo "All tests passed successfully!"
