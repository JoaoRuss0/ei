# Shop API Documentation <!-- omit in toc -->

This documentation provides details on how to interact with the Shop API endpoints.

<details>
<summary>Table Of Contents</summary>

- [GET /Shop](#get-shop)
- [GET /Shop/{id}](#get-shopid)
- [POST /Shop](#post-shop)
- [PUT /Shop/{id}](#put-shopid)
- [DELETE /Shop/{id}](#delete-shopid)
- [GET /Shop/health](#get-shophealth)

</details>

## GET /Shop

Retrieves a list of all shops.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-3-234-223-157.compute-1.amazonaws.com:8080/Shop' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-234-223-157.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns a JSON array of shop objects, each containing the following fields:

```json
[
  {
    "id": <integer>,
    "address": <string>,
    "postalCode": <string>,
    "name": <string>
  },
  ...
]
```

## GET /Shop/{id}

Retrieves a specific shop by ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual shop ID you want to retrieve.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-3-234-223-157.compute-1.amazonaws.com:8080/Shop/{id}' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-234-223-157.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

Response will be a JSON object containing the shop details:

```json
{
  "id": <integer>,
  "address": <string>,
  "postalCode": <string>,
  "name": <string>
}
```

## POST /Shop

Creates a new shop.

Must include a body like this:

```json
{
  "address": <string>,
  "postalCode": <string>,
  "name": <string>
}
```

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'POST' \
>   'http://ec2-3-234-223-157.compute-1.amazonaws.com:8080/Shop' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "address": "string",
>     "postalCode": "string",
>     "name": "string"
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-234-223-157.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns an object with the created shop ID:

```json
{
  "id": <integer>
}
```

## PUT /Shop/{id}

Updates an existing shop by ID.

Must include a body like this:

```json
{
  "address": <string>,
  "postalCode": <string>,
  "name": <string>
}
```

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'PUT' \
>   'http://ec2-3-234-223-157.compute-1.amazonaws.com:8080/Shop/{id}' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "address": "aaaa",
>     "postalCode": "ppppp",
>     "name": "nnnnn"
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-234-223-157.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

No content is returned on success, but the shop is updated with the provided data.

## DELETE /Shop/{id}

Deletes a shop by ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual shop ID you want to delete.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'DELETE' \
>   'http://ec2-3-234-223-157.compute-1.amazonaws.com:8080/Shop/{id}' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-234-223-157.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

No content is returned on success, but the shop is deleted.

## GET /Shop/health

This endpoint checks the health of the Shop microservice.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-54-242-137-75.compute-1.amazonaws.com:8080/Shop/health' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-54-242-137-75.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns a JSON object indicating the health status like this:

```json
{
  "checks": {
    "database": "UP"
  },
  "status": "UP",
  "timestamp": 1749943153750
}
```

If the service or the database are not healthy, the status will reflect that:

- If the service is down, the status will be "DOWN".
- If the database is down, the status will be "DOWN".
