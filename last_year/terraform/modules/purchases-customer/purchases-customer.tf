variable "kafka_brokers" {
  description = "List of Kafka broker public DNS names"
  type        = list(string)
}

variable "rds_address" {
  description = "RDS endpoint address"
  type        = string
}

variable "rds_port" {
  description = "RDS endpoint port"
  type        = number
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

variable "db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "laas"
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

variable "basePath" {
  description = "Path for the user data script"
  type        = string
  default     = "modules/purchases-customer/"
}

variable "module_name" {
  description = "Name of the first module"
  type        = string
  default     = "purchases"
}

variable "module_name2" {
  description = "Name of the second module"
  type        = string
  default     = "customers"
}

resource "aws_instance" "deployQuarkusPurchasesCustomers" {
  depends_on = [null_resource.docker_build]

  ami                    = "ami-08cf815cff6ee258a" # Amazon Linux ARM AMI built by Amazon Web Services
  instance_type          = "t4g.micro"
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name               = "vockey"
  user_data = base64encode(templatefile("${var.basePath}MicroservicesCreation.sh", {
    module_name     = var.module_name
    module_name2    = var.module_name2
    docker_username = var.docker_image_user
    docker_password = var.docker_image_pull_token
    rds_address     = var.rds_address
    rds_port        = var.rds_port
    db_username     = var.db_username
    db_name         = var.db_name
    db_password     = var.db_password
    kafka_brokers   = join(" ", var.kafka_brokers)
  }))
  user_data_replace_on_change = true
  tags = {
    Name = "terraform-deploy-QuarkusPurchases-Customers"
  }
}
resource "aws_security_group" "instance" {
  name = var.security_group_name
  ingress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
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
  default     = "terraform-Quarkus-purchases-customers"
}

resource "null_resource" "docker_build" {
  provisioner "local-exec" {
    command = "docker login -u \"${var.docker_image_user}\" -p \"${var.docker_image_create_token}\" && cd ../microservices/Purchase && ./mvnw clean package -Dquarkus.container-image.group=${var.docker_image_user} -Dquarkus.docker.buildx.platform=linux/arm64,linux/amd64 -Dquarkus.container-image.push=true && cd ../customer && ./mvnw clean package -Dquarkus.container-image.group=${var.docker_image_user} -Dquarkus.docker.buildx.platform=linux/arm64,linux/amd64 -Dquarkus.container-image.push=true"
  }
}

output "purchases-customerAddress" {
  value       = aws_instance.deployQuarkusPurchasesCustomers.public_dns
  description = "Address of the Quarkus EC2 machine"
}

resource "null_resource" "health_check" {
  depends_on = [aws_instance.deployQuarkusPurchasesCustomers]

  connection {
    type        = "ssh"
    host        = aws_instance.deployQuarkusPurchasesCustomers.public_dns
    user        = "ec2-user"
    private_key = file("../labsuser.pem")
    timeout     = "30s"
  }

  provisioner "file" {
    content = templatefile("${var.basePath}setup.sh", {
      publicdns = aws_instance.deployQuarkusPurchasesCustomers.public_dns
    })
    destination = "/tmp/setup.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/setup.sh",
      "sudo /tmp/setup.sh",
      "rm -rf /tmp/setup.sh",
    ]
  }
}
