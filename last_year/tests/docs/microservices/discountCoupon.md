# discountCoupon microservice API test script

This script tests the [discountCoupon microservice API](../../../microservices/docs/APIs/discountCoupon.md) by performing the following actions:

## 1. Get initial discount coupon list

This step tests the `GET /DiscountCoupon` endpoint to retrieve the current list of discount coupons and verify that a JSON array is returned.

## 2. Create a new discount coupon

This step tests the `POST /DiscountCoupon` endpoint to create a new discount coupon with the following data:

```json
{
  "idLoyaltyCard": 1,
  "idsShops": [1, 2, 3],
  "discount": 20,
  "expirationDate": "2025-12-31T23:59:59"
}
```

It then extracts the newly created shop's ID from the response.

## 3. Get all discount coupons after creation

This step tests the `GET /DiscountCoupon` endpoint to ensure that the newly created discount coupon is present in the coupon list.

## 4. Get the created discount coupon by ID

This step tests the `GET /DiscountCoupon/{id}` endpoint to retrieve the newly created discount coupon by its unique ID.

## 5. Delete the discount coupon by ID

This step tests the `DELETE /DiscountCoupon/{id}` endpoint to delete the discount coupon that was created in step 2.

## 6. Get all discount coupons after deletion

This step tests the `GET /DiscountCoupon` endpoint one final time to ensure the coupon was successfully deleted and is no longer present in the list.
