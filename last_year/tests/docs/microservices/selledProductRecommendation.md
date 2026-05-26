# selledProductRecommendation microservice API test script

This script tests the [selledProductRecommendation microservice API](../../../microservices/docs/APIs/selledProductRecommendation.md) by performing the following actions:

## 1. Create a new selled product recommendation

This step tests the `POST /SelledProduct` endpoint to create a new selled product recommendation with the following data:

```json
{
  "typeOfAnalysis": "CUSTOMER",
  "typeValue": "1",
  "timestamp": "2022-03-10T12:15:50",
  "result": 10
}
```
