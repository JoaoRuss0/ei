# Selled Product API Documentation <!-- omit in toc -->

This documentation provides details on how to interact with the Selled Product API endpoints.

This API allows you to send messages with the results of product analysis, which are then published to specific Kafka topics based on the type of analysis.

## POST /SelledProduct

Sends a message with the result of an analysis to a Kafka topic.

Must include a body like this:

```json
{
  "typeOfAnalysis": <string>,
  "typeValue": "<id-or-postal-code>",
  "timestamp": "<date-time>",
  "data": [
    {
      "product": <string>,
      "count":  <number>,
      "sumPrice":  <number>
    },
    {
      "product": <string>,
      "count":  <number>,
      "sumPrice":  <number>
    },
    ...
  ]
}
```

Where:

- `typeOfAnalysis` can be "LOYALTY_CARD", "DISCOUNT_COUPON", "SHOP" or "POSTAL_CODE"
- `typeValue` is the specific value for the analysis type (e.g., loyalty card ID, discount coupon ID, shop ID or postal code)

Depending on the `typeOfAnalysis`, the message will be sent to different Kafka topics:

- For "LOYALTY_CARD", it goes to `selledProductByLoyaltyCard`
- For "DISCOUNT_COUPON", it goes to `selledProductByCoupon`
- For "SHOP", it goes to `selledProductByShop`
- For "POSTAL_CODE", it goes to `selledProductByLocation`

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'POST' \
>   'http://ec2-54-160-190-56.compute-1.amazonaws.com:8082/SelledProduct' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>   "typeOfAnalysis": "LOYALTY_CARD",
>   "typeValue": "12",
>   "timestamp": "2022-03-10T12:15:50",
>   "data": [
>     {
>       "product": 10,
>       "count": 10,
>       "sumPrice": 10
>     },
>     {
>       "product": 20,
>       "count": 20,
>       "sumPrice": 20
>     }
>   ]
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-34-201-0-73.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

If the request is successful, it will return a response body like this:

```
Message sent to Kafka Topic: {typeOfAnalysis:<string>, typeValue:<string>, timestamp:<date-time>, data:{product:<string>, count:<number>, sumPrice:<number>}}
```

## GET /SelledProduct/health

Checks the health status of the Selled Product microservice.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-54-242-137-75.compute-1.amazonaws.com:8082/SelledProduct/health' \
>   -H 'accept: */*'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-54-242-137-75.compute-1.amazonaws.com` on port `8082`. Replace this with your actual instance address if different.
>
> </details>

<br>

If the service is healthy, it will return a response body like this:

```json
{
  "checks": {
    "kafka_servers": "CONFIGURED"
  },
  "status": "UP",
  "timestamp": 1749943213714
}
```

If the service or the Kafka servers are not healthy, the status will reflect that:

- If the service is down, the status will be "DOWN".
- If the Kafka servers are not configured, the Kafka check will indicate "NOT CONFIGURED".
