# microservicesAPIs tests documentation <!-- omit in toc -->

<details>
<summary>Table of Contents</summary>

- [Prerequisites](#prerequisites)
- [Running the tests](#running-the-tests)
  - [Update EC2 Instance DNS Names](#update-ec2-instance-dns-names)
  - [Test Kong](#test-kong)
  - [Run all tests](#run-all-tests)
  - [Give execute permissions to the test scripts](#give-execute-permissions-to-the-test-scripts)
- [Available Scripts](#available-scripts)

</details>

### Prerequisites

- bash
- curl
- grep

### Running the tests

Before running the tests, ensure that:

- The microservice APIs that you want to test are running.
- The EC2 instance DNS name is set correctly in the test scripts.
- The test scripts have execute permissions - see [# Give execute permissions to the test scripts](#give-execute-permissions-to-the-test-scripts).

#### Update EC2 Instance DNS Names

You can automatically update the EC2 instance DNS name in the test scripts by running the following command:

```bash
./update-dns.sh
```

#### Test Kong

To run the tests using Kong, you can run the following script to automatically update the DNS names in the test scripts:

```bash
./use_kong.sh
```

#### Run all tests

To run all the tests (both using Kong and without Kong), execute the following command in your terminal:

```bash
./run_all_tests.sh
```

#### Give execute permissions to the test scripts

You can give execute permissions to the test scripts by running the following command:

```bash
chmod +x ./*.sh
```

### Available Scripts

Test Microservices:

- [crossSellingRecomendations.sh](../../microservicesAPIs/crossSellingRecommendations.sh): Tests the cross-selling recommendations microservice API.
  - Check this [test documentation](crossSellingRecommendations.md) for more details.
- [customers.sh](../../microservicesAPIs/customer.sh): Tests the customer microservice API.
  - Check this [test documentation](customer.md) for more details.
- [discountCoupon.sh](../../microservicesAPIs/discountCoupon.sh): Tests the discount coupon microservice API.
  - Check this [test documentation](discountCoupon.md) for more details.
- [loyaltyCard.sh](../../microservicesAPIs/loyaltyCard.sh): Tests the loyalty card microservice API.
  - Check this [test documentation](loyaltyCard.md) for more details.
- [ollama.sh](../../microservicesAPIs/ollama.sh): Tests the Ollama microservice
  - Check this [test documentation](ollama.md) for more details.
- [purchase.sh](../../microservicesAPIs/purchase.sh): Tests the purchase microservice API.
  - Check this [test documentation](purchase.md) for more details.
- [selledProductRecommendation.sh](../../microservicesAPIs/selledProductRecommendation.sh): Tests the selled product recommendations microservice API.
  - Check this [test documentation](selledProductRecommendations.md) for more details.
- [shops.sh](../../microservicesAPIs/shops.sh): Tests the shops microservice API.
  - Check this [test documentation](shops.md) for more details.

Other scripts:

- [update-dns.sh](../../microservicesAPIs/update-dns.sh): Updates the EC2 instance DNS names and port in the test scripts to use the current EC2 instance DNS name and port.
- [use_kong.sh](../../microservicesAPIs/use_kong.sh): Updates the EC2 instance DNS names and port in the test scripts to use the Kong API Gateway DNS name and port.
- [run_all_tests.sh](../../microservicesAPIs/run_all_tests.sh): Runs all the test scripts in the current directory.
