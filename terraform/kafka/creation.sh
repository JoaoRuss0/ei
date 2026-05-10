#!/bin/bash
echo "Starting..."

cd
sudo yum -y install java-17-amazon-corretto-devel.x86_64
sudo wget https://dlcdn.apache.org/kafka/4.1.1/kafka_2.13-4.1.1.tgz
sudo tar -zxf kafka_2.13-4.1.1.tgz
cd kafka_2.13-4.1.1

TOKEN=`curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds:21600"`

dnsname=`curl -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-hostname`
clusterlist=$(cat /home/ec2-user/cluster_hosts)
oldclusterlist=$(cat /home/ec2-user/cluster_hosts_old_format)

# Cluster
sudo sed -i "s/node.id=1/node.id=${idBroker+1}/g" config/server.properties
sudo sed -i "s/listeners=PLAINTEXT:\/\/:9092,CONTROLLER:\/\/:9093/listeners=PLAINTEXT:\/\/0.0.0.0:9092,CONTROLLER:\/\/0.0.0.0:9093/g" config/server.properties
sudo sed -i "s/advertised.listeners=PLAINTEXT:\/\/localhost:9092,CONTROLLER:\/\/localhost:9093/advertised.listeners=PLAINTEXT:\/\/$dnsname:9092/g" config/server.properties
sudo sed -i "s/controller.quorum.bootstrap.servers=localhost:9093/controller.quorum.bootstrap.servers=$clusterlist/g" config/server.properties

KAFKA_CLUSTER_ID=MkU3OEVBNTcwNTJENDM2M
sudo bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server.properties --initial-controllers $oldclusterlist
echo "sudo bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/server.properties --initial-controllers $oldclusterlist"

# Replication
sudo sed -i "s/broker.id=0/broker.id=${idBroker+1}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/offsets.topic.replication.factor=1/offsets.topic.replication.factor=${totalBrokers}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/transaction.state.log.replication.factor=1/transaction.state.log.replication.factor=${totalBrokers}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/transaction.state.log.min.isr=1/transaction.state.log.min.isr=${totalBrokers}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/^num.partitions=1/num.partitions=${totalBrokers}/g" /usr/local/kafka/config/server.properties
echo "default.replication.factor=${totalBrokers}" >> /usr/local/kafka/config/server.properties

# Startup
sudo bin/kafka-server-start.sh config/server.properties &

echo "Finished."