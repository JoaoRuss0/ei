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
  profile                  = "account_2"
  region                   = "us-east-1"
}

resource "aws_instance" "kong_instance" {
  ami                     = "ami-07ff62358b87c7116"
  instance_type           = "t3.small"
  vpc_security_group_ids  = [aws_security_group.instance.id]
  key_name                = "vockey"

  user_data = templatefile("${path.module}/deploy.sh", {
    MICROSERVICES = var.MICROSERVICES
  })

  user_data_replace_on_change = true

  tags = {
    Name = "terraform-kong"
  }
}

variable "MICROSERVICES" {
  description = "Map of microservice name to base URL to register as Kong services + routes"
  type        = map(string)
  default     = {}
}

resource "aws_security_group" "instance" {
  name = var.security_group_name
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

variable "security_group_name" {
  description = "The name of the security group"
  type        = string
  default     = "terraform-kong-instance2026"
}

output "address" {
  value       = aws_instance.kong_instance.public_dns
  description = "Connect to the KONG at this endpoint"
}


