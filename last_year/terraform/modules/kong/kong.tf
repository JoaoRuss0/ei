variable "basePath" {
  description = "Path for the user data script"
  type        = string
  default     = "modules/kong/"
}

variable "ollama_address" {
  description = "Ollama endpoint address"
  type        = string
}

variable "purchases_customer_address" {
  description = "Purchases and customers endpoint address"
  type        = string
}

variable "discount_coupons_cross_selling_address" {
  description = "Discount Coupon and Cross Selling Recommendations endpoint address"
  type        = string
}

variable "shop_loyaltycard_selled_products_address" {
  description = "Shop, Loyalty Card and Selled Product endpoint address"
  type        = string
}

resource "aws_instance" "InstallKong" {
  ami                    = "ami-045269a1f5c90a6a0"
  instance_type          = "t2.small"
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name               = "vockey"
  user_data = base64encode(templatefile("${var.basePath}deploy.sh", {
    ollama_address                           = var.ollama_address,
    purchases_customer_address               = var.purchases_customer_address,
    discount_coupons_cross_selling_address   = var.discount_coupons_cross_selling_address,
    shop_loyaltycard_selled_products_address = var.shop_loyaltycard_selled_products_address,
  }))

  user_data_replace_on_change = true
  tags = {
    Name = "terraform-Kong"
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
  default     = "terraform-kong-instance"
}

output "KongAddress" {
  value       = aws_instance.InstallKong.public_dns
  description = "Connect to the Kong at this endpoint"
}

resource "null_resource" "kongSetup" {
  depends_on = [aws_instance.InstallKong]

  connection {
    type        = "ssh"
    host        = aws_instance.InstallKong.public_dns
    user        = "ec2-user"
    private_key = file("../labsuser.pem")
    timeout     = "30s"
  }

  provisioner "file" {
    content = templatefile("${var.basePath}setup.sh", {
      publicdns = aws_instance.InstallKong.public_dns
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
