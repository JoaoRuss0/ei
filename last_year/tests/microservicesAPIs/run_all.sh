#!/bin/bash

TEST_SCRIPTS=(
  "crossSellingRecomendations.sh"
  "customer.sh"
  "discountCoupon.sh"
  "loyaltyCard.sh"
  "ollama.sh"
  "purchase.sh"
  "selledProductRecommendation.sh"
  "shops.sh"
)

FAILED_TESTS=()

CURRENTLY_RUNNING=""

run_tests() {
  echo "Running all tests"

  for script in "${TEST_SCRIPTS[@]}"; do
      echo
      echo
      echo
      echo "Running $script"
      if ./"$script"; then
        echo
        echo "$script -> PASSED"
      else
        echo
        echo "$script -> FAILED"
        FAILED_TESTS+=( "$CURRENTLY_RUNNING - $script" )
      fi  done
}

echo "Updating DNS to use the EC2 instance"
CURRENTLY_RUNNING="Microservices APIs Tests"
./update-dns.sh

run_tests

echo "Updating to use Kong as API Gateway"
CURRENTLY_RUNNING="Kong API Gateway Tests"
cd .. && ./use_kong.sh && cd microservicesAPIs

run_tests

echo
echo
echo "Finished running all tests."

if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
  echo "All tests passed successfully!"
else
  echo "The following tests failed:"
  for script in "${FAILED_TESTS[@]}"; do
    echo "- $script"
  done
fi
