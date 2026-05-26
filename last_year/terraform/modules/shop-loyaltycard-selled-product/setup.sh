#!/bin/bash
echo "Waiting for Shop service to be ready (DB UP)..."
until curl -s http://${publicdns}:8080/Shop/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8080/Shop/health | grep -q '"database":"UP"'; do
  sleep 2
done
echo "Shop Service is ready!"

echo "Waiting for Loyalty Card service to be ready (DB UP)..."
until curl -s http://${publicdns}:8081/Loyaltycard/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8081/Loyaltycard/health | grep -q '"database":"UP"'; do
  sleep 2
done
echo "Loyalty Card Service is ready!"

echo "Waiting for Selled Product service to be ready (Kafka CONFIGURED)..."
until curl -s http://${publicdns}:8082/SelledProduct/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8082/SelledProduct/health | grep -q '"kafka_servers":"CONFIGURED"'; do
  sleep 2
done
echo "Selled Product Service is ready!"