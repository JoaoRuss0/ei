1ST SPRINT

The first sprint consists in the definition of the information flows, in the setup of the kafka cluster, in the development of the microservices (that could be based on the already provided microservices) and testing of all the development components.

The expected deliverables are:

1. Design your own understanding of the information flow considering the requirements and the architecture of VPPaaS. We suggest you specify the Kafka topics and partitions, as well as the microservices to be used using UML sequence diagrams (https://plantuml.com/) - but any other representation is accepted.

2. Setup and deployment of the Kafka Cluster using TERRAFORM Suggestion: for testing purposes use the VPPaaS producer (https://github.com/Enterprise-Integration-IST-2026/VPPaaS-EventProducer) and create the needed topics by command line

3. The implementation of the following microservices, using ((Quarkus or AWSLambda) and AWS RDS) and TERRAFORM (and excluding the Camunda and Kong – business process implementation is a deliverable for 2nd sprint):

o Prosumer

o UtilityOperator

o AssetLink

o Telemetry Ingestion

o Flexibility Emission

o Grid Balancing

o Energy Analytics

o Flexibility Forecasting (AI)

4. Documentation of tests considering all the previous deliverables

5. Documentation for the source code, terraform scripts files, installation procedures, and

parametrizations.
