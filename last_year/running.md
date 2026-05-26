# How to run this project

## Prerequisites

- Terraform
- Docker + Buildx
- Java 17
- Maven
- AWS account with CLI access configured

## Initialize the project

### 1. Configure AWS credentials

Add your AWS credentials to the `.aws/credentials` file

### 2. Configure Terraform variables

Create a `terraform/terraform.tfvars` file, using the `terraform/terraform.tfvars.example` as a template.

### 3. Configure SSH access

Ensure that you have SSH access to the AWS instances. Add your RSA private key to the `labsuser.pem` file.

### 4. Start docker

Make sure Docker is running and you have the necessary permissions to run Docker commands.

### 5. Run the `init_project.sh` script

This script will initialize the project, create the necessary Docker images, and set up the Terraform state.

## Destroy the project

To destroy the project, run the `destroy_project.sh` script. This will remove all resources created by Terraform.