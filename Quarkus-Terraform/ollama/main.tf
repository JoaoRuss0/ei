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
  region = "us-east-1"
}

resource "aws_instance" "exampleDeployOllama" {
  ami                    = "ami-0244b3aa0b9e167c7"
  instance_type          = "t3.medium"
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name               = "vockey"
  user_data              = file("ollama.sh")
  user_data_replace_on_change = true

  root_block_device {
    volume_size = 20
  }

  tags = {
    Name = "terraform-ollama"
  }
}

resource "aws_security_group" "instance" {
  name = "terraform-ollama-sg"
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 11434
    to_port     = 11434
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

output "public_dns" {
  value = aws_instance.exampleDeployOllama.public_dns
}
