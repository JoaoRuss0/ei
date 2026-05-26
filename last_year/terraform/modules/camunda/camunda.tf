variable "basePath" {
  description = "Path for the user data script"
  type        = string
  default     = "modules/camunda/"
}

variable "kong_address" {
  description = "Kong endpoint address"
  type        = string
}

resource "aws_instance" "InstallCamundaEngine" {
  ami                    = "ami-045269a1f5c90a6a0"
  instance_type          = "t2.small"
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name               = "vockey"
  user_data = base64encode(templatefile("${var.basePath}deploy.sh", {
    kong_address = var.kong_address,
  }))
  user_data_replace_on_change = true
  tags = {
    Name = "terraform-Camunda"
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
  default     = "terraform-Camunda-instance"
}
output "CamundaAddress" {
  value       = aws_instance.InstallCamundaEngine.public_dns
  description = "Connect to Camunda at this endpoint"
}

resource "null_resource" "camundaSetup" {
  provisioner "local-exec" {
    command = <<EOT
    chmod +x ${var.basePath}setup.sh
    publicdns_camunda=${aws_instance.InstallCamundaEngine.public_dns} ${var.basePath}setup.sh
    EOT
  }

  depends_on = [aws_instance.InstallCamundaEngine]
}
