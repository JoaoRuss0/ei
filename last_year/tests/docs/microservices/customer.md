# customer microservice API test script

This script tests the [customer microservice API](../../../microservices/docs/APIs/customer.md) by performing the following actions:

## 1. Get initial customer list

This step tests the `GET /Customer` endpoint to retrieve the current list of customers and verify that a JSON array is returned.

## 2. Create a new customer

This step tests the `POST /Customer` endpoint to create a new customer with the following data:

```json
{
  "FiscalNumber": 0,
  "address": "string",
  "postalCode": "string",
  "name": "string"
}
```

It then extracts the newly created shop's ID from the response.

## 3. Get all customers after creation

This step tests the `GET /Customer` endpoint to ensure that the newly created customer is present in the customer list.

## 4. Get the created customer by ID

This step tests the `GET /Customer/{id}` endpoint to retrieve the newly created customer by their unique ID.

## 5. Update the customer by ID

This step tests the `PUT /Customer/{id}` endpoint to update the customer created in step 2 with the following data:

```json
{
  "FiscalNumber": 1,
  "address": "aaaaaa",
  "postalCode": "ppppp",
  "name": "nnnnn"
}
```

## 6. Get the updated customer

This step tests the `GET /Customer/{id}` endpoint again to confirm that the customer’s information has been updated correctly.

## 7. Delete the customer by ID

This step tests the `DELETE /Customer/{id}` endpoint to delete the customer that was created created in step 2 and updated in the previous steps.

## 8. Get all customers after deletion

This step tests the `GET /Customer` endpoint one final time to ensure the customer was successfully deleted and is no longer present in the list.
