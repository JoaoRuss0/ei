#!/bin/bash

TERRAFORM_DIR="../terraform"
TEST_DIR="$(pwd)"
MICROSERVICES_DIR="./microservicesAPIs"
KONG_KEY="KongAddress"
DEFAULT_PORT=8000

scripts=(
  "populate-databases.sh"
  "$MICROSERVICES_DIR/crossSellingRecomendations.sh"
  "$MICROSERVICES_DIR/customer.sh"
  "$MICROSERVICES_DIR/discountCoupon.sh"
  "$MICROSERVICES_DIR/loyaltyCard.sh"
  "$MICROSERVICES_DIR/ollama.sh"
  "$MICROSERVICES_DIR/purchase.sh"
  "$MICROSERVICES_DIR/selledProductRecommendation.sh"
  "$MICROSERVICES_DIR/shops.sh"
)

terraform_output=$(cd "$TERRAFORM_DIR" && terraform output)

kong_address=$(echo "$terraform_output" | grep "^$KONG_KEY" | grep -oE "ec2-[^\"]+")

if [[ -z "$kong_address" ]]; then
  echo "Kong address not found in Terraform output."
  exit 1
fi

echo "Kong address found: $kong_address"

for script in "${scripts[@]}"; do
  script_path="$TEST_DIR/$script"

  if [[ -f "$script_path" ]]; then
    echo "Updating $script_path with Kong address: $kong_address and PORT: $DEFAULT_PORT"

    sed -i.bak -E "s|^EC2_DNS=\"[^\"]*\"|EC2_DNS=\"$kong_address\"|" "$script_path"

    sed -i.bak -E "s|^PORT=[0-9]+|PORT=$DEFAULT_PORT|" "$script_path"

    rm -f "$script_path.bak"
  else
    echo "Script $script_path not found."
  fi
done

echo "All scripts updated with new Kong address and PORT values."
