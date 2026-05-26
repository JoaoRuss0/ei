# Kafka Tests Documentation

## Prerequisites

Kafka and microservices must be running.

See the [running.md](running.md) for instructions on how to run the project.

## Running the tests

### 1. Create topic with the Purchase Microservice

Use the `POST /Purchase/Consume` (see documentation [here](../../microservices/docs/APIs/purchase.md#post-purchaseconsume)) or just run the `tests/microservicesAPIs/purchase.sh` script to create the topic (see test documentation [here](./microservices/purchase.md)).

### 2. Transfer the test JAR to the EC2 instance

You can use the following command to transfer the test JAR to the EC2 instance - ideally one of the kafka brokers:

```bash
scp -i /path/to/key.pem /path/to/yourfile/LaaSSimulator2025-0.0.1-shade.jar\
ec2-user@your-ec2-public-ip:/destination/path/
```

### 3. Access the EC2 instance

SSH into the EC2 instance where you transferred the JAR file:

```bash
ssh -i /path/to/key.pem ec2-user@your-ec2-public-ip
```

### 4. Run the test JAR

```bash
java -jar LaaSSimulator2025-0.0.1-shade.jar \
--broker-list kafka01.example.com:9092,kafka02.example.com:9092,kafka03.example.com:9092 \
--throughput 100
```

Here, `--broker-list` specifies the addresses of all Kafka brokers, and `--throughput` defines the approximate number of messages to produce per minute. Optionally, the `--filterprefix` flag can be used to restrict production to topics with a specific prefix.

By leaving the simulator running, we can evaluate Kafka’s performance and throughput. Additionally, sending data to multiple topics helps verify the correct behavior of the Kafka cluster. To assess resiliency, we can deliberately shut down one of the brokers during simulation and confirm that the system continues operating as expected.
