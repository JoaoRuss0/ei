#!/bin/bash

EC2_DNS="ec2-18-205-116-184.compute-1.amazonaws.com"
PORT=8000
API_BASE_URL="http://$EC2_DNS:$PORT"

function post_request {
	local endpoint="$1"
	local post_data="$2"
	local response=$(curl -s -X POST "$API_BASE_URL/$endpoint" \
		-H 'accept: application/json' \
		-H 'Content-Type: application/json' \
		-d "$post_data")
}

function create_customer {
	local endpoint="Customer"

	local name="$1"
	local fiscal_number="$2"
	local address="$3"
	local postal_code="$4"
	local post_data='{
		"name": "'"$name"'",
		"fiscalNumber": "'"$fiscal_number"'",
		"address": "'"$address"'",
		"postalCode": "'"$postal_code"'"
	}'

	post_request "$endpoint" "$post_data"
}

function create_shop {
	local endpoint="Shop"

	local name="$1"
	local address="$2"
	local postal_code="$3"
	local post_data='{
		"name": "'"$name"'",
		"address": "'"$address"'",
		"postalCode": "'"$postal_code"'"
	}'

	post_request "$endpoint" "$post_data"
}

function create_loyalty_card {
	local endpoint="Loyaltycard"

	local customer_id="$1"
	local shop_id="$2"
	local post_data='{
		"idCustomer": '"$customer_id"',
		"idShop": '"$shop_id"'
	}'

	post_request "$endpoint" "$post_data"
	create_purchase_topic "$customer_id-$shop_id"
}

function create_purchase_topic {
	local endpoint="Purchase/Consume"

	local topic_name="$1"
	local post_data='{
		"TopicName": "'"$topic_name"'"
	}'

	local response=$(curl -s -X POST "$API_BASE_URL/$endpoint" \
		-H 'accept: text/plain' \
		-H 'Content-Type: application/json' \
		-d "$post_data")

}

function create_discount_coupon {
	local endpoint="DiscountCoupon"

	local idLoyaltyCard="$1"
	local idsShops="$2"
	local discount="$3"
	local expirationDate="$4"
	local post_data='{
		"idLoyaltyCard": '"$idLoyaltyCard"',
		"idsShops": '"$idsShops"',
		"discount": '"$discount"',
		"expirationDate": "'"$expirationDate"'"
	}'

	post_request "$endpoint" "$post_data"
}

create_customer "Customer 1" "1" "Customer 1 Address" "postalCode1"
create_customer "Customer 2" "2" "Customer 2 Address" "postalCode2"
create_customer "Customer 3" "3" "Customer 3 Address" "postalCode3"

create_shop "Shop 1" "Shop 1 Address" "Same"
create_shop "Shop 2" "Shop 2 Address" "Same"
create_shop "Shop 3" "Shop 3 Address" "Different"

create_loyalty_card 1 1
create_loyalty_card 1 2
create_loyalty_card 1 3
create_loyalty_card 2 1
create_loyalty_card 2 2
create_loyalty_card 3 3

create_discount_coupon 1 "[1,2,3]" 20 "2025-12-31T23:59:59"
