terraform {
  required_version = ">= 1.0.0, < 2.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    bucket                   = "terraform-ie-project-bucket"
    key                      = "terraform.tfstate"
    region                   = "us-east-1"
    shared_credentials_files = ["../.aws/credentials"]
  }
}

provider "aws" {
  region                   = "us-east-1"
  shared_credentials_files = ["../.aws/credentials"]
}

variable "nBroker" {
  description = "number of brokers"
  type        = number
  default     = 3
}

variable "db_username" {
  description = "The username for the database"
  type        = string
  sensitive   = true
  default     = "ie_project"
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
  default     = "password"
}

variable "db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "laas"
}

variable "docker_image_user" {
  description = "The username to use for the docker image"
  type        = string
  default     = "docker_image_user"
}

variable "docker_image_pull_token" {
  description = "The password/token to use to pull the docker image"
  type        = string
  default     = "password"
}

variable "docker_image_create_token" {
  description = "The password/token to use to create the docker image"
  type        = string
  default     = "password"
}

module "kafka-cluster" {
  source  = "./modules/kafka-cluster"
  nBroker = var.nBroker
}

module "rds" {
  source      = "./modules/rds"
  db_username = var.db_username
  db_password = var.db_password
  db_name     = var.db_name
}

module "ollama" {
  source = "./modules/ollama"
}

output "kafka_ips" {
  value = module.kafka-cluster.publicdnslist
}

output "rds_address" {
  value = module.rds.address
}

output "rds_port" {
  value = module.rds.port
}

output "ollama_address" {
  value = module.ollama.address
}

module "purchases-customers" {
  source                    = "./modules/purchases-customer"
  kafka_brokers             = module.kafka-cluster.publicdnslist
  rds_address               = module.rds.address
  rds_port                  = module.rds.port
  db_username               = var.db_username
  db_password               = var.db_password
  db_name                   = var.db_name
  docker_image_create_token = var.docker_image_create_token
  docker_image_pull_token   = var.docker_image_pull_token
  docker_image_user         = var.docker_image_user

  depends_on = [
    module.kafka-cluster,
    module.rds,
  ]
}

output "purchases-customerAddress" {
  value = module.purchases-customers.purchases-customerAddress
}

module "shop-loyaltycard-selledProduct" {
  source                    = "./modules/shop-loyaltycard-selled-product"
  kafka_brokers             = module.kafka-cluster.publicdnslist
  rds_address               = module.rds.address
  rds_port                  = module.rds.port
  db_username               = var.db_username
  db_password               = var.db_password
  db_name                   = var.db_name
  docker_image_create_token = var.docker_image_create_token
  docker_image_pull_token   = var.docker_image_pull_token
  docker_image_user         = var.docker_image_user
  depends_on = [
    module.rds,
    module.kafka-cluster,
  ]
}

output "shop-loyaltycard-selledProductAddress" {
  value = module.shop-loyaltycard-selledProduct.shop-loyaltycard-selledProductAddress
}

module "discountCoupon-crossSellingRecommendation" {
  source                    = "./modules/discount-coupon-cross-selling-recommendation"
  kafka_brokers             = module.kafka-cluster.publicdnslist
  rds_address               = module.rds.address
  rds_port                  = module.rds.port
  db_username               = var.db_username
  db_password               = var.db_password
  db_name                   = var.db_name
  docker_image_create_token = var.docker_image_create_token
  docker_image_pull_token   = var.docker_image_pull_token
  docker_image_user         = var.docker_image_user
  depends_on = [
    module.kafka-cluster,
    module.rds,
  ]
}

output "discountCoupon-crossSellingRecommendationAddress" {
  value = module.discountCoupon-crossSellingRecommendation.discountCoupon-crossSellingRecommendationAddress
}

module "Kong" {
  source                                   = "./modules/kong"
  ollama_address                           = module.ollama.address[0]
  purchases_customer_address               = module.purchases-customers.purchases-customerAddress
  discount_coupons_cross_selling_address   = module.discountCoupon-crossSellingRecommendation.discountCoupon-crossSellingRecommendationAddress
  shop_loyaltycard_selled_products_address = module.shop-loyaltycard-selledProduct.shop-loyaltycard-selledProductAddress
  depends_on = [
    module.discountCoupon-crossSellingRecommendation,
    module.shop-loyaltycard-selledProduct,
    module.purchases-customers,
    module.ollama,
  ]
}

output "KongAddress" {
  value = module.Kong.KongAddress
}

module "Camunda" {
  source       = "./modules/camunda"
  kong_address = module.Kong.KongAddress
  depends_on = [
    module.Kong,
  ]
}

output "CamundaAddress" {
  value = module.Camunda.CamundaAddress
}
