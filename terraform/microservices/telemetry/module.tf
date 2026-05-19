variable "DATA_SOURCE" {}
variable "KAFKA_CLUSTER" {}
variable "DOCKER_USERNAME" {}
variable "DOCKER_PASSWORD" {}

module "telemetry" {
  source = "../"
  SERVICE_NAME = "telemetry"
  ACCOUNT_NAME = "account_2"
  DATA_SOURCE = var.DATA_SOURCE
  KAFKA_CLUSTER = var.KAFKA_CLUSTER
  DOCKER_USERNAME = var.DOCKER_USERNAME
  DOCKER_PASSWORD = var.DOCKER_PASSWORD
}