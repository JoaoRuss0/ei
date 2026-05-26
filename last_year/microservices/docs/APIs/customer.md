# customer API Documentation <!-- omit in toc -->

This documentation provides details on how to interact with the Customer API endpoints.

This API allows you to manage customer data, including creating, reading, updating, and deleting customer records.

<details>
<summary>Table Of Contents</summary>

- [GET /Customer](#get-customer)
- [GET /Customer/{id}](#get-customerid)
- [POST /Customer](#post-customer)
- [PUT /Customer/{id}](#put-customerid)
- [DELETE /Customer/{id}](#delete-customerid)
- [GET /Customer/health](#get-customerhealth)

</details>

## GET /Customer

Retrieves a list of all customers.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-44-193-226-242.compute-1.amazonaws.com:8080/Customer' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-44-193-226-242.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns a JSON array of customer objects, each containing the following fields:

```json
[
  {
    "FiscalNumber": <integer>,
    "address": <string>,
    "postalCode": <string>,
    "name": <string>
  },
  ...
]
```

## GET /Customer/{id}

Retrieves a specific customer by ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual customer ID you want to retrieve.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-44-193-226-242.compute-1.amazonaws.com:8080/Customer/{id}' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-44-193-226-242.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns a JSON object containing the customer details:

```json
{
  "FiscalNumber": <integer>,
  "address": <string>,
  "postalCode": <string>,
  "name": <string>
}
```

## POST /Customer

Creates a new customer.

Must include a body like this:

```json
{
  "FiscalNumber": <integer>,
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
>   'http://ec2-44-193-226-242.compute-1.amazonaws.com:8080/Customer' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "FiscalNumber": 0,
>     "address": "string",
>     "postalCode": "string",
>     "name": "string"
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-44-193-226-242.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

Returns an object with the created customer ID.

```json
{
  "id": <integer>
}
```

## PUT /Customer/{id}

Updates an existing customer by ID.

You must replace `{id}` with the actual customer ID you want to update and must include a body like this:

```json
{
  "FiscalNumber": <integer>,
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
>   'http://ec2-44-193-226-242.compute-1.amazonaws.com:8080/Customer/{id}' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "FiscalNumber": 1,
>     "address": "aaaaaa",
>     "postalCode": "ppppp",
>     "name": "nnnnn"
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-44-193-226-242.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

No content is returned on success, but the customer is updated with the provided data.

## DELETE /Customer/{id}

Deletes a customer by ID.

No payload is required for this endpoint, but you must replace `{id}` with the actual customer ID you want to delete.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'DELETE' \
>   'http://ec2-44-193-226-242.compute-1.amazonaws.com:8080/Customer/{id}' \
>   -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-44-193-226-242.compute-1.amazonaws.com` on port `8080`. Don't forget to replace this with your actual instance's public DNS or IP address.
>
> </details>

<br>

No content is returned on success, but the customer is deleted.

## GET /Customer/health

This endpoint checks the health of the Customer microservice.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>  'http://ec2-34-201-143-220.compute-1.amazonaws.com:8081/Customer/health' \
>  -H 'accept: application/json'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-34-201-143-220.compute-1.amazonaws.com` on port `8081`. Don't forget to replace this with your actual instance's public DNS or IP address.
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
  "timestamp": 1749941993631
}
```

If the service or the database are not healthy, the status will reflect that:

- If the service is down, the status will be "DOWN".
- If the database is down, the status will be "DOWN".
