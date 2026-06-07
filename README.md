# VPPaaS — Virtual Power Plant as a Service

A microservice-based platform that orchestrates prosumers, utility operators, grid cells, assets, and telemetry to produce energy analytics, grid balancing recommendations, flexibility offers, and AI-assisted flexibility forecasting. The platform is deployed on AWS via Terraform and orchestrated by Camunda 8 (Zeebe) over a Kong API gateway, with Kafka as the event backbone.

## Repository layout

```
.
├── README.md                          ← you are here
├── deploy.sh                          ← provisions all AWS infra + microservices + Camunda artifacts
├── undeploy.sh                        ← tears every Terraform module down in parallel
├── build_and_push.sh                  ← builds Docker images and hot-swaps them on the live EC2 instances
├── redeploy_camunda_artifacts.sh      ← re-uploads BPMN / DMN / forms to a running Camunda
├── addresses.sh                       ← resolves and exports DNS/host vars from terraform state
├── test.sh                            ← runs the integration-test suite
│
├── bpmn/                              ← Camunda 8 artifacts grouped by business process
│   ├── asset/                         ← Asset CRUD process + forms
│   ├── assetlink/                     ← AssetLink + Kafka topic provisioning process
│   ├── energy-analytics/              ← Analytics aggregation BPMN + forms
│   ├── flexibility-emission/          ← BPMN + DMN rules for flexibility events
│   ├── flexibility-forecasting/       ← Ollama-driven AI forecasting process
│   ├── grid-balancing-recommendation/ ← Grid-balancing recommendation BPMN
│   ├── gridcell/                      ← GridCell CRUD process + forms
│   ├── prosumer/                      ← Prosumer CRUD process + forms
│   └── utilityoperator/               ← UtilityOperator CRUD process + forms
│
├── microservices/                     ← Quarkus services (reactive MySQL + smallrye-kafka)
│   ├── Asset/
│   ├── AssetLink/                     ← also owns Kafka topic lifecycle
│   ├── EnergyAnalytics/               ← consumes via /save, emits to 4 analytics topics
│   ├── FlexibilityEmission/           ← emits flexibility-offers
│   ├── GridBalancingRecommendation/   ← emits balancing-recommendation
│   ├── GridCell/
│   ├── Prosumer/
│   ├── Telemetry/                     ← dynamic Kafka consumer per AssetLink topic
│   └── UtilityOperator/
│
├── terraform/                         ← one Terraform module per piece of infrastructure
│   ├── rds/                           ← MySQL RDS
│   ├── kafka/                         ← Kafka cluster (EC2)
│   ├── camunda/                       ← Camunda 8 Zeebe + Operate (EC2)
│   ├── kong/                          ← Kong API gateway
│   ├── konga/                         ← Konga UI for Kong
│   └── microservices/                 ← one sub-module per Quarkus service
│       ├── asset/, asset_link/, energy_analytics/, …
│       ├── ollama/                    ← Ollama LLM host for flexibility forecasting
│       ├── main.tf, quarkus.sh        ← shared module + EC2 bootstrap
│
├── integration-tests/                 ← end-to-end scripts driven by curl + kafka-console-consumer
│   ├── _lib.sh                        ← shared helpers (capture_next_message / await_captured_message)
│   ├── asset-lifecycle.sh             ← CRUD lifecycle: GET-all → POST → GET → PUT → GET → DELETE → GET-404
│   ├── asset-link-lifecycle.sh        ← CRD + by-prosumer-id / by-utilityoperator-id lookups (no PUT)
│   ├── prosumer-lifecycle.sh          ← CRUD lifecycle for Prosumer
│   ├── grid-cell-lifecycle.sh         ← CRUD lifecycle for GridCell (string ID, UNIQUE coord triple)
│   ├── utility-operator-lifecycle.sh  ← CRUD lifecycle for UtilityOperator (UNIQUE iban)
│   ├── topic-creation-workflow.sh     ← full lifecycle: create entities → topic → consume → cleanup
│   ├── flexibility-emission.sh        ← POST FlexibilityEvent → read flexibility-offers
│   ├── grid-balancing.sh              ← POST /balance → read balancing-recommendation
│   ├── energy-analytics.sh            ← POST /save → read energy-discharged-by-zone
│   ├── ollama.sh                      ← AI forecast smoke test
│   ├── balance_payload.json           ← fixture for grid-balancing.sh
│   └── payload.json                   ← fixture for ollama.sh
│
├── event-producer/                    ← Java fat-jar that emits synthetic telemetry to a Kafka topic
├── kafka-binary/                      ← bundled Apache Kafka CLI (consumer, producer, admin tools)
├── diagrams/                          ← PlantUML diagrams for each process
├── config/                            ← SSH keys + Docker credentials (gitignored)
│   ├── docker_variables.sh            ← export DOCKER_USERNAME / DOCKER_PASSWORD
│   ├── docker_variables.sh.example
│   ├── key.pem, key2.pem              ← EC2 SSH keys for refresh
│   ├── credentials                    ← AWS creds
│   └── global-bundle.pem              ← RDS CA bundle
├── logs/                              ← per-step deployment + per-service refresh logs
└── report_sprint_1.pdf, statement.pdf
```

## Prerequisites

- `terraform`, `aws` CLI configured (credentials in `~/.aws/` or `config/credentials`)
- `docker` running locally (for `build_and_push.sh`)
- `jq`, `curl`, `ssh`
- Java 17+, Maven (or use the `mvnw` wrapper bundled in each service)
- `config/docker_variables.sh` populated:
  ```bash
  export DOCKER_USERNAME="<dockerhub-user>"
  export DOCKER_PASSWORD="<dockerhub-token-or-password>"
  ```
- `config/key.pem` and `config/key2.pem` present with `chmod 600`

## Full deploy

Provisions RDS + Kafka + Ollama + 9 Quarkus microservices + Kong + Konga + Camunda, then creates Kafka topics and uploads BPMN/DMN/forms.

```bash
./deploy.sh
```

What it does, in order:
1. Spins up **RDS, Kafka, Ollama** in parallel (logs in `logs/{rds,kafka,ollama}.log`).
2. Spins up **all 9 microservices** in parallel, passing the RDS + Kafka addresses (`logs/<service>.log`) and creates the six needed Kafka topics.
3. Generates `terraform/kong/microservices.auto.tfvars.json` with every service's DNS, deploys **Kong**.
4. Deploys **Camunda** and **Konga** in parallel.
5. Waits for Camunda to be ready, then POSTs every `.form`, `.dmn`, and `.bpmn` under `bpmn/` to its `/v2/deployments` endpoint.

After it finishes, each public URL should be printed.
To get these addresses, you can also run `./addresses.sh` or `source ./addresses.sh`, if you feel the need to populate `$KAFKA_CLUSTER`, `$DB_ADDRESS`, `$KONG_URL`, `$CAMUNDA_URL`, and each `$<SERVICE>_URL`.

## Full undeploy

```bash
./undeploy.sh
```

Runs `terraform destroy -auto-approve` on every terraform component in parallel.
Per-module logs go to `logs/<name>_destroy.log`. Idempotent — safe to re-run on a partial state.

## Remote-update a specific microservice

Build the service's Quarkus container image locally, push it to Docker Hub, then SSH into its EC2 instance and replace the running container — without re-running Terraform.

```bash
# Update one service:
./build_and_push.sh Telemetry

# Update several:
./build_and_push.sh Asset AssetLink GridCell

# Update all 9:
./build_and_push.sh
```

Flags:
- `--no-refresh` — build + push the image, skip the remote container restart.
- `--refresh-only` — skip the build, just pull-and-restart the existing image on EC2.

Pipeline per service:
1. `mvn clean package -DskipTests -Dquarkus.container-image.group=$DOCKER_USERNAME` (Quarkus builds + pushes the image as part of `package`).
2. `source addresses.sh` resolves the EC2 DNS for the service from Terraform state.
3. SSHes in with `config/<key>.pem`, runs `docker pull`, `docker stop`, `docker rm`, `docker run` with the updated image and the same env vars (DB URL, Kafka brokers).

Per-service Maven and SSH output is captured to `logs/<Service>.log`. Setup output (Docker login, address resolution) goes to `logs/setup.log`.

> **Note:** the live container's `myapp.schema.create` defaults to `true`, so a restart drops and re-seeds the service's table. Stop containers explicitly if you need to preserve state.

## Redeploy Camunda BPMN / DMN / forms

After editing anything under `bpmn/`, push it to a running Camunda without touching the EC2 instance:

```bash
./redeploy_camunda_artifacts.sh
```

It resolves the Camunda DNS from Terraform state (or honors `$addressCamunda` if exported), waits for `/v2/topology` to respond, then uploads in this order:
1. All `*.form` files
2. All `*.dmn` files
3. All `*.bpmn` files

Camunda versions each deployment automatically — new process instances pick up the latest version while in-flight instances finish on their original version.

## Running the test suite

```bash
./test.sh
```
Runs all `microservices/` unit tests, then the integration-test scripts under `integration-tests/`.

Integration scripts run in this order against the deployed services:

1. **CRUD lifecycle smoke tests** — `asset-lifecycle.sh`, `asset-link-lifecycle.sh`, `prosumer-lifecycle.sh`, `grid-cell-lifecycle.sh`, `utility-operator-lifecycle.sh`. Each one walks read-all → create → read-one → update → read-one → delete → read-one (expect 404), asserts the expected HTTP status at every step, and registers a `trap … EXIT` so the created row is removed even if an assertion fails mid-flight. Payloads are chosen so they never collide with seeded data or with any `UNIQUE` constraint. `asset-link-lifecycle.sh` substitutes the update step with `by-prosumer-id` and `by-utilityoperator-id` lookups, since `AssetLink` has no `PUT` endpoint.
2. **Event-driven scenarios** — `topic-creation-workflow.sh`, `flexibility-emission.sh`, `grid-balancing.sh`, `energy-analytics.sh`, `ollama.sh`. Each one:
   - Pre-positions a Kafka consumer on the target topic via `_lib.sh::capture_next_message`
   - POSTs to trigger the producer
   - Awaits the message via `await_captured_message`
   - Deletes every entity it created (reverse-creation order)

`topic-creation-workflow.sh` additionally provisions an AssetLink Kafka topic, runs the bundled `event-producer.jar` against it, verifies Telemetry persisted the row, then stops the Telemetry consumer, deletes the topic, and tears down all five entities it created.

### Running unit tests for a single microservice

`test.sh` loops over every service in `microservices/`. To run the unit tests for **just one** service, invoke its bundled Maven wrapper directly:

```bash
# Run the full unit-test suite of one service
cd microservices/Telemetry && ./mvnw clean test
```

Replace `Telemetry` with any of `Asset`, `AssetLink`, `EnergyAnalytics`, `FlexibilityEmission`, `GridBalancingRecommendation`, `GridCell`, `Prosumer`, or `UtilityOperator`.

Each service's tests are `@QuarkusTest`s that spin up a real MySQL 8.0 container via Quarkus DevServices (`%test.quarkus.datasource.devservices.enabled=true` in `application.properties`), so **Docker must be running locally**. No deployed AWS infrastructure is required — the suite is fully self-contained.

## Misc

```bash
# Export every public address into the current shell
source ./addresses.sh

# Tail a service's deploy log
tail -f logs/telemetry.log

# Consume from a topic
./kafka-binary/bin/kafka-console-consumer.sh \
    --bootstrap-server "$KAFKA_CLUSTER" \
    --include "balancing-recommendation|flexibility-offers|energy-discharged-by-zone|generated-energy-by-prosumer|consumed-energy-by-prosumer|average-soc" \
    --from-beginning
```
