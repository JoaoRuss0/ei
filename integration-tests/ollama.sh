#!/bin/bash

cd ..
source addresses.sh > /dev/null
cd integration-tests

echo "-------------------------------------------------"
echo "Checking Ollama response..."
echo "-------------------------------------------------"

API_URL="http://$OLLAMA_URL/api/generate"

echo "Sending forecasting request to Ollama..."
curl -X POST "$API_URL" -H "Content-Type: application/json" -d @payload.json

echo "-------------------------------------------------"
echo ""
echo ""
echo ""
echo ""