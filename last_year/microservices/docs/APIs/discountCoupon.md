# Discount Coupon API Documentation <!-- omit in toc -->

This documentation provides details on how to interact with the Discount Coupon API endpoints.

This API allows you to manage discount coupons, including retrieving, creating, and deleting them.

<details>
<summary>Table Of Contents</summary>

- [GET /DiscountCoupon](#get-discountcoupon)
- [GET /DiscountCoupon/{id}](#get-discountcouponid)
- [POST /DiscountCoupon](#post-discountcoupon)
- [DELETE /DiscountCoupon/{id}](#delete-discountcouponid)
- [GET /DiscountCoupon/health](#get-discountcouponhealth)

</details>

## GET /DiscountCoupon

Retrieves all discount coupons.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-3-221-149-118.compute-1.amazonaws.com:8080/DiscountCoupon' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-221-149-118.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

Returns a JSON array of discount coupon objects, each containing the following fields:

```json
[
  {
    "id": <integer>,
    "idLoyaltyCard": <integer>,
    "idsShops": [<integer>, <integer>, ...],
    "discount": <integer>,
    "expirationDate": "YYYY-MM-DDTHH:MM:SS"
  },
  ...
]
```

## GET /DiscountCoupon/{id}

Retrieves a specific discount coupon by its unique ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual discount coupon ID you want to retrieve.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-3-221-149-118.compute-1.amazonaws.com:8080/DiscountCoupon/{id}' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-221-149-118.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

Response will be a JSON object containing the discount coupon details:

```json
{
  "id": <integer>,
  "idLoyaltyCard": <integer>,
  "idsShops": [<integer>, <integer>, ...],
  "discount": <integer>,
  "expirationDate": "YYYY-MM-DDTHH:MM:SS"
}
```

## POST /DiscountCoupon

Creates a discount coupon for a given loyalty card and a list of shops.

Must include a body like this:

```json
{
  "idLoyaltyCard": <integer>,
  "idsShops": [<integer>, <integer>, ...],
  "discount": <integer>,
  "expirationDate": "YYYY-MM-DDTHH:MM:SS"
}
```

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'POST' \
>   'http://ec2-3-221-149-118.compute-1.amazonaws.com:8080/DiscountCoupon' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "idLoyaltyCard": 1,
>     "idsShops": [1, 2, 3],
>     "discount": 20,
>     "expirationDate": "2022-03-10T12:15:50"
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-221-149-118.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

Returns an object with the created discount coupon ID:

```json
{
  "id": <integer>
}
```

## DELETE /DiscountCoupon/{id}

Deletes a discount coupon by ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual discount coupon ID you want to delete.

<details>
<summary>Curl Example</summary>

```bash
curl -X 'DELETE' \
  'http://ec2-3-221-149-118.compute-1.amazonaws.com:8080/DiscountCoupon/{id}' \
  -H 'accept: application/json'
```

> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-221-149-118.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.

</details>

<br>

No content is returned on success, but the discount coupon is deleted.

## GET /DiscountCoupon/health

This endpoint checks the health of the Discount Coupon microservice.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-52-23-238-172.compute-1.amazonaws.com:8080/DiscountCoupon/health' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-52-23-238-172.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

Returns a JSON object indicating the health status like this:

```json
{
  "checks": {
    "kafka_servers": "CONFIGURED",
    "database": "UP"
  },
  "status": "UP",
  "timestamp": 1749942265190
}
```

If the service, the database or the Kafka servers are not healthy, the status will reflect that:

- If the service is down, the status will be "DOWN".
- If the database is down, the database check will indicate "DOWN".
- If the Kafka servers are not configured, the Kafka check will indicate "NOT CONFIGURED".
