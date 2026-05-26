# loyaltycard microservice API test script

This script tests the [loyaltycard microservice API](../../../microservices/docs/APIs/loyaltycard.md) by performing the following actions:

## 1. Get initial loyalty card list

This step tests the `GET /Loyaltycard` endpoint to retrieve the current list of loyalty cards and verify that a JSON array is returned.

## 2. Create a new loyalty card

This step tests the `POST /Loyaltycard` endpoint to create a new loyalty card with the following data:

```json
{
  "idCustomer": 1,
  "idShop": 1
}
```

It then extracts the newly created card's ID from the response.

## 3. Get all loyalty cards and confirm the new card is present

This step tests the `GET /Loyaltycard` endpoint again to verify the presence of the newly created card using its ID.

## 4. Get the loyalty card by ID

This step tests the `GET /Loyaltycard/{id}` endpoint to retrieve the card by its unique ID.

## 5. Get the loyalty card by Customer ID and Shop ID

This step tests the `GET /Loyaltycard/{idCustomer}/{idShop}` endpoint to retrieve the card by composite key.

## 6. Delete the loyalty card by Customer ID and Shop ID

This step tests the `DELETE /Loyaltycard/{idCustomer}/{idShop}` endpoint to remove the card using its composite key.

## 7. Confirm deletion via GET all

This step tests the `GET /Loyaltycard` endpoint again to confirm the card is no longer present.

## 8. Create another loyalty card

This step repeats the `POST /Loyaltycard` to create a new loyalty card using the same data:

```json
{
  "idCustomer": 1,
  "idShop": 1
}
```

## 9. Delete the new card by ID

This step tests the `DELETE /Loyaltycard/{id}` endpoint to delete the newly created card using its unique ID.

## 10. Confirm final deletion

This step performs a final `GET /Loyaltycard` request to verify that the card is no longer present.
