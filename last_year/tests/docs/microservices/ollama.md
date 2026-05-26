# ollama microservice API test script

This script tests the ollama microservice by performing the following action:

## Retrieve a response from the ollama microservice
This step tests the `POST /api/generate` endpoint to send a request to the ollama microservice with the following data:
```json
{
  "model": "llama3.2",
  "prompt": "Why is the sky blue?",
  "stream": false
}
```