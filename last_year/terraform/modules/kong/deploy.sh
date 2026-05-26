#!/bin/bash
echo "Starting..."
sudo yum install -y docker
sudo service docker start
sudo docker network create kong-net
sudo docker run -d --name kong-database \
--network=kong-net \
-p 5432:5432 \
-e "POSTGRES_USER=kong" \
-e "POSTGRES_DB=kong" \
-e "POSTGRES_PASSWORD=kongpass" \
postgres:13
sudo docker run --rm --network=kong-net \
-e "KONG_DATABASE=postgres" \
-e "KONG_PG_HOST=kong-database" \
-e "KONG_PG_PASSWORD=kongpass" \
-e "KONG_PASSWORD=test" \
kong/kong-gateway:3.9.0.0 kong migrations bootstrap
sudo docker run -d --name kong-gateway \
--network=kong-net \
-e "KONG_DATABASE=postgres" \
-e "KONG_PG_HOST=kong-database" \
-e "KONG_PG_USER=kong" \
-e "KONG_PG_PASSWORD=kongpass" \
-e "KONG_PROXY_ACCESS_LOG=/dev/stdout" \
-e "KONG_ADMIN_ACCESS_LOG=/dev/stdout" \
-e "KONG_PROXY_ERROR_LOG=/dev/stderr" \
-e "KONG_ADMIN_ERROR_LOG=/dev/stderr" \
-e "KONG_ADMIN_LISTEN=0.0.0.0:8001, 0.0.0.0:8444 ssl" \
-e "KONG_ADMIN_GUI_URL=http://localhost:8002" \
-e KONG_LICENSE_DATA \
-p 8000:8000 \
-p 8443:8443 \
-p 8001:8001 \
-p 8002:8002 \
-p 8445:8445 \
-p 8003:8003 \
-p 8004:8004 \
-p 127.0.0.1:8444:8444 \
kong/kong-gateway:3.9.0.0

# Wait until Kong Gateway Admin API is up
echo "Waiting for Kong Gateway to be ready..."
until curl -s -o /dev/null -w "%%{http_code}" http://localhost:8001 | grep -q "200"; do
  sleep 5
done
echo "Kong Admin Gateway is ready!"

# Set the Ollama address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=ollama' --data "url=http://${ollama_address}:11434"

# Set the Customer address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=customer' --data "url=http://${purchases_customer_address}:8081"

# Set the Purchases address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=purchases' --data "url=http://${purchases_customer_address}:8080"

# Set the Shop address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=shop' --data "url=http://${shop_loyaltycard_selled_products_address}:8080"

# Set the Loyalty Card address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=loyaltycard' --data "url=http://${shop_loyaltycard_selled_products_address}:8081"

# Set the Selled Product address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=selled-product' --data "url=http://${shop_loyaltycard_selled_products_address}:8082"

# Set the Discount Coupon address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=discount-coupon' --data "url=http://${discount_coupons_cross_selling_address}:8080"

# Set the Cross Selling Recommendation address
curl -i -X POST --url http://localhost:8001/services/ --data 'name=cross-selling-recommendation' --data "url=http://${discount_coupons_cross_selling_address}:8081"

# Set the Route for Ollama
curl -i -X POST --url "http://localhost:8001/services/ollama/routes" --data "paths[]=~/api/generate" --data "strip_path=false"

# Set the Route for Customer
curl -i -X POST --url "http://localhost:8001/services/customer/routes" --data "paths[]=~/Customer(/.*)?" --data "strip_path=false"

# Set the Route for Purchases
curl -i -X POST --url "http://localhost:8001/services/purchases/routes" --data "paths[]=~/Purchase(/.*)?" --data "strip_path=false"

# Set the Route for Shop
curl -i -X POST --url "http://localhost:8001/services/shop/routes" --data "paths[]=~/Shop(/.*)?" --data "strip_path=false"

# Set the Route for Loyalty Card
curl -i -X POST --url "http://localhost:8001/services/loyaltycard/routes" --data "paths[]=~/Loyaltycard(/.*)?" --data "strip_path=false"

# Set the Route for Selled Product
curl -i -X POST --url "http://localhost:8001/services/selled-product/routes" --data "paths[]=~/SelledProduct(/.*)?" --data "strip_path=false"

# Set the Route for Discount Coupon
curl -i -X POST --url "http://localhost:8001/services/discount-coupon/routes" --data "paths[]=~/DiscountCoupon(/.*)?" --data "strip_path=false"

# Set the Route for Cross Selling Recommendation
curl -i -X POST --url "http://localhost:8001/services/cross-selling-recommendation/routes" --data "paths[]=~/CrossSellingRecommendation(/.*)?" --data "strip_path=false"

echo "Finished."