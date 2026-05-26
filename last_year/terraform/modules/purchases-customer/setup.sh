#!/bin/bash
echo "Waiting for Purchases service to be ready (DB UP + Kafka CONFIGURED)..."
until curl -s http://${publicdns}:8080/Purchase/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8080/Purchase/health | grep -q '"database":"UP"' && curl -s http://${publicdns}:8080/Purchase/health | grep -q '"kafka_servers":"CONFIGURED"'; do
  sleep 2
done
echo "Purchases Service is ready!"

echo "Waiting for Customer service to be ready (DB UP)..."
until curl -s http://${publicdns}:8081/Customer/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8081/Customer/health | grep -q '"database":"UP"'; do
  sleep 2
done
echo "Customer Service is ready!"