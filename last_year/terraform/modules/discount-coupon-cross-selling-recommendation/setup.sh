#!/bin/bash
echo "Waiting for Discount Coupon service to be ready (DB UP + Kafka CONFIGURED)..."
until curl -s http://${publicdns}:8080/DiscountCoupon/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8080/DiscountCoupon/health | grep -q '"database":"UP"' && curl -s http://${publicdns}:8080/DiscountCoupon/health | grep -q '"kafka_servers":"CONFIGURED"'; do
  sleep 2
done
echo "Discount Coupon Service is ready!"

echo "Waiting for Cross Selling service to be ready (Kafka CONFIGURED)..."
until curl -s http://${publicdns}:8081/CrossSellingRecommendation/health | grep -q '"status":"UP"' && curl -s http://${publicdns}:8081/CrossSellingRecommendation/health | grep -q '"kafka_servers":"CONFIGURED"'; do
  sleep 2
done
echo "Cross Selling Service is ready!"