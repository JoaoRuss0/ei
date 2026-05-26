# Cross-Selling Recommendation API Documentation <!-- omit in toc -->

This documentation provides details on how to interact with the Cross-Selling Recommendation API endpoints.

This API allows you to generate cross-selling recommendations based on customer loyalty card IDs and shop IDs.

## POST /CrossSellingRecommendation

Generates a cross-selling recommendation based on a loyalty card ID and a list of shop IDs, sending it to the `crossSellingRecommendation` Kafka topic.

Must include a body like this:

```json
{
  "idLoyaltyCard": <integer>,
  "idsShops": [<integer>, <integer>, ...]
}
```

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'POST' \
>   'http://ec2-3-237-79-145.compute-1.amazonaws.com:8080/CrossSellingRecommendation' \
>   -H 'accept: application/json' \
>   -H 'Content-Type: application/json' \
>   -d '{
>     "idLoyaltyCard": 0,
>     "idsShops": [1, 2, 3]
> }'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-3-237-79-145.compute-1.amazonaws.com` on port `8080`. Replace this with your actual instance address if different.
>
> </details>

<br>

If the request is successful, it will return a response body like this:

```
Message sent to Kafka Topic: {idLoyaltyCard: <integer>, idsShops: [<integer>,  ...]}
```

## GET /CrossSellingRecommendation/health

This endpoint checks the health status of the Cross-Selling Recommendation microservice.

No payload is required for this endpoint.

> <details>
> <summary>Curl Example</summary>
>
> ```bash
> curl -X 'GET' \
>   'http://ec2-52-23-238-172.compute-1.amazonaws.com:8081/CrossSellingRecommendation/health' \
>   -H 'accept: */*'
> ```
>
> In this example, the EC2 instance is accessed via its public DNS name `ec2-52-23-238-172.compute-1.amazonaws.com` on port `8081`. Replace this with your actual instance address if different.
>
> </details>

<br>

Returns a JSON object indicating the health status like this:

```json
{
  "checks": {
    "kafka_servers": "CONFIGURED"
  },
  "status": "UP",
  "timestamp": 1749942692900
}
```

If the service or the Kafka servers are not healthy, the status will reflect that:

- If the service is down, the status will be "DOWN".
- If the Kafka servers are not configured, the Kafka check will indicate "NOT CONFIGURED".
