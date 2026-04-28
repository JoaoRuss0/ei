# Input variables for the terraform files - var.<variable name>

variable "security_group_name" {
    description = "The name of the security group"
    type        = string
    default     = "terraform-example-2-cluster"
}

variable "nBroker" {
    description = "number of brokers"
    type        = number
    default     = 3
}