terraform {
  required_version = ">= 1.0.0, < 2.0.0"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 4.0" }
  }
}

provider "aws" {
  shared_credentials_files = ["${path.module}/../../config/credentials"]
  profile                  = "account_1"
  region                   = "us-east-1"
}

variable "nBroker" { default = 3 }

resource "aws_instance" "kafkaCluster" {
  ami                    = "ami-07ff62358b87c7116"
  instance_type          = "t3.small"
  count                  = var.nBroker
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name               = "vockey"

  user_data = base64encode(
    templatefile("creation.sh", {
      idBroker     = count.index
      totalBrokers = var.nBroker
    })
  )

  user_data_replace_on_change = true
  tags                        = { Name = "kafka-broker.${count.index}" }
}

resource "aws_security_group" "instance" {
  name = "kafka-cluster-sg"
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 9093
    to_port     = 9093
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

locals {
  replica_directorys = [
    "r1eMpKZRROex80kgn4_2-g",
    "bWIe6tPFS3mKvq-W-_kgtQ",
    "Z57HkCAASdifFa-QWCdGWA"
  ]
  quorum_voters = join(",", [
    for i, instance in aws_instance.kafkaCluster :
    "${i + 1}@${instance.public_dns}:9093:${local.replica_directorys[i]}"
  ])
}

resource "null_resource" "update_dns" {
  count = var.nBroker
  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file("${path.module}/../../config/key.pem")
    host        = aws_instance.kafkaCluster[count.index].public_ip
  }
  provisioner "remote-exec" {
    inline = [
      "echo -n '${join(":9093,", aws_instance.kafkaCluster.*.public_dns)}:9093' > cluster_hosts",
      "echo -n '${local.quorum_voters}' > cluster_hosts_old_format"
    ]
  }
}

output "publicdnslist" {
  value = aws_instance.kafkaCluster.*.public_dns
}

