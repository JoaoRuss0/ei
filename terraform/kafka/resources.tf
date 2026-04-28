resource "aws_instance" "exampleCluster" {
    ami                    = "ami-07ff62358b87c7116"
    instance_type          = "t3.small"
    count                  = var.nBroker
    vpc_security_group_ids = [aws_security_group.instance.id]
    key_name               = "vockey"
    user_data = base64encode(
        templatefile("creation.sh", {
            idBroker     = "${count.index}"
            totalBrokers = var.nBroker }
        )
    )
    user_data_replace_on_change = true
    tags                        = { Name = "terraform-example-2-kafka.${count.index}" }
}

resource "aws_security_group" "instance" {
    name = var.security_group_name
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

resource "null_resource" "update_dns" {
    count = var.nBroker

    connection {
        type        = "ssh"
        user        = "ec2-user"
        private_key = file("../.aws/key.pem")
        host        = aws_instance.exampleCluster[count.index].public_ip
    }

    provisioner "remote-exec" {
        inline = [
            "echo -n '${join(":9093,", aws_instance.exampleCluster.*.public_dns)}' > cluster_hosts",
            "echo -n ':9093' >> cluster_hosts",
            "cat cluster_hosts",
            "echo -n '${local.quorum_voters}' > cluster_hosts_old_format",
            "cat cluster_hosts_old_format"
        ]
    }
}
