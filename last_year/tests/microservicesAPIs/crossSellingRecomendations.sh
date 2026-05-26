#!/bin/bash

EC2_DNS="ec2-184-72-178-17.compute-1.amazonaws.com"
PORT=8000
API_URL="http://$EC2_DNS:$PORT/CrossSellingRecommendation"

response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d '{"idLoyaltyCard": 0, "idsShops": [1,2,3]}')
echo "POST recommendation response: $response"

echo "All tests passed successfully!"