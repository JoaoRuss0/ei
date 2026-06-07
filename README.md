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

## Seeded data — start any business process out of the box

Every microservice's `initdb()` populates its table with a small but **internally consistent fixture** the first time the service boots. Together they form a complete graph (Prosumers ↔ AssetLinks ↔ UtilityOperators, Assets owned by Prosumers, GridCells owned by Operators, Telemetry tied to Assets and Cells) so you can kick off every BPMN process **without creating a single row by hand**.

Headline figures:

| Entity              | Seeded rows                                                                                                  |
| ------------------- | ------------------------------------------------------------------------------------------------------------ |
| `Prosumer`          | 4 — `Maria Lisbon` (1), `Joao Setubal` (2), `Pedro Porto` (3), `Ana Faro` (4)                                |
| `UtilityOperator`   | 4 — `ArcoCegoLisbon` (1), `PracadeBocage` (2), `PracadaBoavista` (3), `PracaDomFranciscoGomes` (4)           |
| `Asset`             | 9 — three per city, mixing `BATTERY` / `SOLAR` / `EV_CHARGER`                                                |
| `GridCell`          | 6 — `PORTO_NORTH`, `PORTO_SOUTH`, `LISBON_NORTH`, `LISBON_SOUTH`, `SETUBAL_CENTRO`, `FARO_CENTRO`            |
| `AssetLink`         | 5 — one self-link per (prosumer, operator) pair + a cross-link `(Maria Lisbon → PracadaBoavista)`            |
| `Telemetry`         | 15 — spread across April 15/20/22/25 and May 30 2026; pre-seeded `Status` / `Plug_Status` / kW values        |
| Kafka topics        | 5 AssetLink topics (`1-ArcoCegoLisbon`, `2-PracadeBocage`, `3-PracadaBoavista`, `4-PracaDomFranciscoGomes`, `5-PracadaBoavista`) auto-created on AssetLink startup and consumed by Telemetry on startup |

The next sections give you concrete BPMN form inputs that are **guaranteed to produce real events** on top of this seeded data — no extra telemetry simulation required.

### Flexibility Emission — guaranteed to emit

The DMN classifies events as `SELL` when `In Peak Hours = true AND State of Charge ≥ 0.9 AND Asset Type = BATTERY`, and as `UNAVAILABLE_FOR_BALANCING` when `State of Charge ≤ 0.2 AND Asset Type = BATTERY`.

Start the `FlexibilityEmission` process and on the prosumer-id form, pick:

- **`prosumerId = 3` (Pedro Porto)** → asset `6` (`porto-battery-1`, BATTERY in `PORTO_NORTH`, peak hours 18:00–21:00) has seeded telemetry at `2026-04-15T19:25` with `SoC=0.92` and `19:35` with `SoC=0.85`. The `19:25` event sits inside Porto's peak window with SoC ≥ 0.9 → DMN fires `SELL`, the microservice POSTs the event and publishes it to the `flexibility-offers` Kafka topic. ✅
- **`prosumerId = 1` (Maria Lisbon)** → asset `1` (`lisbon-battery-1`, BATTERY in `LISBON_NORTH`) has a seeded `OFFLINE`, `SoC=0.20` row at `2026-04-22T03:00`. SoC ≤ 0.2 fires `UNAVAILABLE_FOR_BALANCING` independently of peak hours. ✅

Verify via the Kafka topic:
```bash
./kafka-binary/bin/kafka-console-consumer.sh --bootstrap-server "$KAFKA_CLUSTER" \
    --topic flexibility-offers --from-beginning
```

### Grid Balancing Recommendation — guaranteed to emit

The seeded `2026-05-30 19:00` batch deliberately overloads `PORTO_NORTH` (`maxLoad = 50 kW`):

| Telemetry event on `PORTO_NORTH`        | Contribution to load |
| --------------------------------------- | -------------------- |
| Asset 5 — SOLAR, `Current_Generation = 8`  | `−8`                  |
| Asset 6 — BATTERY, ONLINE, `Current_Output = 0` | `0`                   |
| Asset 7 — EV_CHARGER, CHARGING, `Charging_Rate = 80` | `+80`                |
| **Net load**                            | **`+72 kW` → overload of 22 kW above maxLoad 50** |

Neighbour cells available for transfer (same coords-grid):
- `PORTO_SOUTH` at `(0,1)`, `maxLoad = 60`, no events → headroom 60 kW.
- `LISBON_NORTH` at `(1,0)`, `maxLoad = 80`, no events → headroom 80 kW.

Start the `GridBalancingRecommendation` process and on the operator form pick:
- **`utilityOperatorId = 3` (PracadaBoavista)** — owner of both Porto cells, so the algorithm sees both the overloaded cell *and* a neighbour with headroom. → emits a `{from: PORTO_NORTH, to: PORTO_SOUTH, transfer_kw: ≈22}` record to the `balancing-recommendation` topic. ✅

Verify:
```bash
curl "$GRID_BALANCING_URL/GridBalancing" | jq '.[-1]'
# or
./kafka-binary/bin/kafka-console-consumer.sh --bootstrap-server "$KAFKA_CLUSTER" \
    --topic balancing-recommendation --from-beginning
```

### Energy Analytics — guaranteed to emit on all four channels

The `EnergyAnalytics` BPMN supports four analysis types; the seeded telemetry is enough to make every one of them produce non-zero values.

- **`ENERGY_DISCHARGED_BY_ZONE` for `PORTO_NORTH`** — asset 6 has a non-zero `Current_Output = 15` row at `2026-04-15T19:35` → emits to topic `energy-discharged-by-zone`.
- **`ENERGY_GENERATED_BY_PROSUMER` for prosumer 3 (Pedro Porto)** — assets 5 and 8 are SOLAR with seeded `Current_Generation` values (8 kW, 5 kW respectively) → emits to topic `generated-energy-by-prosumer`.
- **`ENERGY_CONSUMED_BY_PROSUMER` for prosumer 3 (Pedro Porto)** — asset 7 (`porto-ev-1`) has `CHARGING` events with `Charging_Rate = 80` → emits to topic `consumed-energy-by-prosumer`.
- **`AVERAGE_SOC` for `PORTO_NORTH`** — asset 6 has multiple SoC samples (`0.92`, `0.85`) → emits to topic `average-soc`.

Verify all four at once:
```bash
./kafka-binary/bin/kafka-console-consumer.sh --bootstrap-server "$KAFKA_CLUSTER" \
    --include "energy-discharged-by-zone|generated-energy-by-prosumer|consumed-energy-by-prosumer|average-soc" \
    --from-beginning
```

> **Tip.** All of the above work because `myapp.schema.create=true` is the default in `application.properties`, so any service restart re-runs `initdb()`. If you want to test against accumulated state instead, set the property to `false` before redeploying — see the note in the *Remote-update a specific microservice* section.

## Telemetry resilience — `TopicSubscriptionRecoveryService`

Telemetry's per-AssetLink Kafka consumers are not just spawned on the fly by the `AssetLinkCreate` BPMN — every active consumer is also **persisted** to a `TopicSubscription` table (`topic_name PRIMARY KEY`, `owner_service`). This is what makes the service surviveable across restarts and lets a healthy instance take over for a failed one without re-running any BPMN.

### How it works

- Every Telemetry instance has a unique `ServiceId.SERVICE_ID` (UUID) generated at startup.
- When the AssetLink BPMN calls `POST /Telemetry/consume`, the `KafkaConsumerService`:
  1. inserts a row into `TopicSubscription` with `owner_service = <this instance's UUID>`,
  2. spins up a `DynamicTopicConsumer` thread for the topic,
  3. registers the worker in the in-memory `KafkaDynamicConsumerTracker`.
- `POST /Telemetry/stop` reverses the process — it only deletes the row if the current instance owns it, so two instances can't accidentally steal each other's subscriptions.

### Recovery on startup

On boot, every Telemetry instance runs `TopicSubscriptionRecoveryService.onStartup()`. It reads the config property:

```properties
recovery.failed-service-uuids=<comma-separated UUIDs of dead instances>
```

For every UUID listed, it queries `TopicSubscription` for topics owned by that UUID and **takes over each one**:

```java
private void takeOver(String topicName) {
    TopicSubscription.updateOwnerService(client, topicName, ServiceId.SERVICE_ID).await().indefinitely();
    DynamicTopicConsumer worker = new DynamicTopicConsumer(topicName, kafkaServers, client);
    worker.start();
    tracker.track(new Topic(topicName), worker);
}
```

After the takeover the new instance is the owner of record, its tracker holds the live consumer thread, and the old UUID is no longer referenced. Telemetry ingestion resumes for the orphaned topics without operator intervention from Camunda.

### Operating notes

- **Empty/unset** `recovery.failed-service-uuids` is the normal case — no recovery runs, the instance only consumes topics created by BPMN flows after its own startup. The seeded topics (`1-ArcoCegoLisbon`, …, `5-PracadaBoavista`) are picked up at boot via the separate seed-topic logic, not via recovery.
- The property can be set per-instance via `application.properties`, or via `JAVA_TOOL_OPTIONS="-Drecovery.failed-service-uuids=<uuid1>,<uuid2>"` on the live container — useful when you've identified a dead pod and want a sibling to inherit its work.
- Because the table is shared (every Telemetry instance points at the same RDS schema), **only one** instance should be told to recover a given UUID at a time — otherwise multiple workers would race to take ownership. In practice you only nominate a single recovery target.
- Subscriptions can also be audited from the DB:
  ```bash
  mysql -h "$DB_ADDRESS" -u <user> -p TelemetryDB \
      -e "SELECT topic_name, owner_service FROM TopicSubscription;"
  ```

