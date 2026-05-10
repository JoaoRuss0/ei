#!/bin/bash

cd ..
source addresses.sh > /dev/null
cd integration-tests

API_URL="http://$OLLAMA_URL/api/generate"

echo "Sending forecasting request to Ollama..."
response=$(curl -s -X POST "$API_URL" -H "Content-Type: application/json" -d @payload.json)
[ -z "$response" ] && { echo "Test failed: No response from server"; exit 1; }

echo "Received response from Ollama:"
echo "$response"

echo "-------------------------------------------------"
