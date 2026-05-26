# Business Processes Tests Documentation

This document provides instructions on how to run the business process tests in the project without any additional setup, specifically for the Cross-Selling Recommendation, Discount Coupon Emission, Discount Coupon Analysis and Selled Products Analysis business processes.

## Populate Databases

To do this, you can use the provided script `populate-databases.sh` which will create the necessary data in the databases.

Please don't forget to update the `KONG_ADDRESS` variable in the script to point to your Kong instance or just run the `use-kong.sh` script to update the Kong configuration.

## Generate Purchases

After running the `populate-databases.sh` script the purchases topics already exist in Kafka, so now you can generate purchases using the `LaaSSimulator2025` JAR file.

Follow the steps below or check the [Kafka tests documentation](kafka.md) for more details.

### Transfer the test JAR to the EC2 instance:

```bash
scp -i labsuser.pem tests/LaaSSimulator2025-0.0.1-shade.jar ec2-user@__KAFKA_ADDRESS__:/tmp/LaaSSimulator2025-0.0.1-shade.jar
```

### Access the EC2 instance:

```bash
ssh -i labsuser.pem ec2-user@__KAFKA_ADDRESS__
```

### Run the test JAR:

```bash
java -jar /tmp/LaaSSimulator2025-0.0.1-shade.jar --broker-list __KAFKA_ADDRESS_1__:9092,__KAFKA_ADDRESS_2__:9092,__KAFKA_ADDRESS_3__:9092 --throughput 100
```

### Some useful commands

#### Listing all topics

```bash
sudo /usr/local/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

#### Consuming messages from a topic with multiple partitions

```bash
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic __TOPIC_NAME__ --from-beginning
```

## Start Business Process

Go to Camunda Tasklist (`http://<CAMUNDA_ADDRESS>:8080/camunda/app/tasklist/default/#/login`) and start the business process you want to test.
