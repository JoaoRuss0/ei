# Enterprise Integration Project

This project goal is to develop an information system for a Loyalty-As-A-Service (LaaS) provider.

Check out the [project statement](project-statement.pdf) for more details.

## Project Structure

```
/
├── bpmn/                   # BPMN files for business processes
├── microservices/          # Quarkus-based microservices (Customer, Shop, LoyaltyCard, etc.)
├── terraform/              # Main Terraform project
├── terraform/modules/      # Terraform modules for infrastructure components
├── tests/                  # Integration test scripts and simulator JAR
├── init_project.sh         # Script to bootstrap infrastructure and deploy services
└── destroy_project.sh      # Script to tear down infrastructure
```

Check the documentation for business processes in the [bpmn/docs](bpmn/docs) directory, the microservices in the [microservices/docs](microservices/docs) directory, and the tests in the [tests/docs](tests/docs/) directory.

## Run the project

See [`running.md`](running.md) for detailed instructions on how to run this project.

## Deliveries

- Find the report for the first sprint [here](report_sprint_1.pdf).
- Find the report for the second sprint [here](report_sprint_2.pdf).
