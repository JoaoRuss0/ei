#!/bin/bash

EC2_DNS="ec2-3-93-17-130.compute-1.amazonaws.com"
PORT=8082
API_URL="http://$EC2_DNS:$PORT/SelledProduct"

post_data='{
  "typeOfAnalysis": "CUSTOMER",
  "typeValue": "1",
  "timestamp": "2022-03-10T12:15:50",
  "data": [
    {
      "productId": 10,
      "count": 10,
      "sumPrice": 100
    },
    {
      "productId": 20,
      "count": 20,
      "sumPrice": 200
    }
  ]
}'
response=$(curl -s -X POST "$API_URL" -H 'accept: application/json' -H 'Content-Type: application/json' -d "$post_data")
echo "POST selled product response: $response"
if [[ $response == *"Message sent to Kafka Topic"* ]]; then
  echo "All tests passed successfully!"
else
  echo "Test failed: Response did not contain expected text"
fi
