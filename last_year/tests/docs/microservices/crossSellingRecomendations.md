# crossSellingRecomendations microservice API test script

This script tests the [crossSellingRecomendations microservice API](../../../microservices/docs/APIs/crossSellingRecomendations.md) by performing the following actions:

## 1. Create a new cross-selling recommendation

This step tests the `POST /CrossSellingRecommendation` endpoint to create a new cross-selling recommendation with the following data:

```json
{
  "idLoyaltyCard": 0,
  "idsShops": [1, 2, 3]
}
```
