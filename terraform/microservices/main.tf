variable "SERVICE_NAME" {}
variable "ACCOUNT_NAME" {}
variable "DATA_SOURCE" {}
variable "KAFKA_CLUSTER" {}
variable "DOCKER_USERNAME" {}
variable "DOCKER_PASSWORD" {}

terraform {
  required_version = ">= 1.0.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  shared_credentials_files = ["${path.module}/../../config/credentials"]
  profile                  = var.ACCOUNT_NAME
  region                   = "us-east-1"
}

resource "aws_instance" "quarkus_instance" {
  ami                     = "ami-0bb7267a511c0a8e8"
  instance_type           = "t4g.small"
  vpc_security_group_ids  = [aws_security_group.instance.id]
  key_name                = "vockey"

  user_data = templatefile("${path.module}/quarkus.sh", {
    DOCKER_USERNAME = var.DOCKER_USERNAME
    DOCKER_PASSWORD = var.DOCKER_PASSWORD
    SERVICE_NAME = var.SERVICE_NAME
    DATA_SOURCE = var.DATA_SOURCE
    KAFKA_CLUSTER = var.KAFKA_CLUSTER
  })

  user_data_replace_on_change = true

  tags = {
    Name = "terraform-${var.SERVICE_NAME}"
  }
}

resource "aws_security_group" "instance" {
  name = "${var.SERVICE_NAME}-sg"
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}
