# shop microservice API test script

This script tests the [shop microservice API](../../../microservices/docs/APIs/shop.md) by performing the following actions:

## 1. Get initial shop list

This step tests the `GET /Shop` endpoint to retrieve the current list of shops and verify that a JSON array is returned.

## 2. Create a new shop

This step tests the `POST /Shop` endpoint to create a new shop with the following data:

```json
{
  "name": "string",
  "address": "string",
  "postalCode": "string"
}
```

It then extracts the newly created shop's ID from the response.

## 3. Get all shops and confirm the new shop is present

This step tests the `GET /Shop` endpoint again to verify the presence of the newly created shop using its ID.

## 4. Get the created shop by ID

This step tests the `GET /Shop/{id}` endpoint to retrieve the shop by the ID obtained during creation.

## 5. Update the shop by ID

This step tests the `PUT /Shop/{id}` endpoint to update the shop with the following data:

```json
{
  "address": "aaaa",
  "postalCode": "ppppp",
  "name": "nnnnn"
}
```

## 6. Confirm the shop was updated

This step tests the `GET /Shop/{id}` endpoint again to verify that the fields `address`, `postalCode`, and `name` were successfully updated.

## 7. Delete the shop by ID

This step tests the `DELETE /Shop/{id}` endpoint to remove the shop.

## 8. Confirm the shop has been deleted

This step tests the `GET /Shop` endpoint once more to confirm the shop no longer appears in the list.
