# Printed to console after running `$ terraform apply`

output "publicdnslist" {
    value = formatlist("%v", aws_instance.exampleCluster.*.public_dns)
}