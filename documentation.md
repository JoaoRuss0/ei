# Sprint 1 — Implementation Documentation

## Deliverable 3: Microservice Implementation

All 8 microservices are implemented using **Quarkus 3.x**, deployed on **AWS EC2** via **Terraform**, and connected to a shared **AWS RDS MySQL** database. Inter-service communication uses **Apache Kafka** (KRaft mode, version 4.1.1).

---

## Infrastructure Overview

| Component | Technology | AWS Resource |
|-----------|-----------|-------------|
| Database | MySQL 8.0 | RDS db.t4g.micro |
| Message Broker | Apache Kafka 4.1.1 | EC2 t3.small |
| AI Inference | Ollama (llama3.2:1b) | EC2 t3.medium |
| Microservices (×8) | Quarkus 3.x / Docker | EC2 t3.micro (×8) |

### Kafka Topics

| Topic | Partitions | Producer | Consumers |
|-------|-----------|----------|-----------|
| `asset-link-events` | 3 | AssetLink | Telemetry |
| `telemetry-events` | 3 | Telemetry | FlexibilityEmission, GridBalancing, EnergyAnalytics |
| `flexibility-events` | 3 | FlexibilityEmission | FlexibilityForecasting |
| `flexibility-offers` | 3 | FlexibilityEmission | — |
| `flexibility-forecasts` | 3 | FlexibilityForecasting | — |
| `balancing-recommendations` | 3 | GridBalancing | — |
| `energy-discharged-by-zone` | 3 | EnergyAnalytics | — |
| `generated-energy-by-prosumer` | 3 | EnergyAnalytics | — |
| `consumed-energy-by-prosumer` | 3 | EnergyAnalytics | — |
| `average-soc` | 3 | EnergyAnalytics | — |
| `invalid-telemetry-events` | 1 | Telemetry | — |

---

## Microservices

### 1. Prosumer
**EC2:** `t3.micro` | **Path:** `microservices/Prosumer`

Manages prosumer registration and profiles. No Kafka integration — purely CRUD over RDS.

**Endpoints:**
- `GET /Prosumer` — list all prosumers
- `GET /Prosumer/{id}` — get by ID
- `POST /Prosumer` — create
- `PUT /Prosumer/{id}` — update
- `DELETE /Prosumer/{id}` — delete

**Database table:** `Prosumer`

---

### 2. UtilityOperator
**EC2:** `t3.micro` | **Path:** `microservices/UtilityOperator`

Manages utility operator registration. No Kafka integration — purely CRUD over RDS.

**Endpoints:**
- `GET /UtilityOperator` — list all
- `GET /UtilityOperator/{id}` — get by ID
- `POST /UtilityOperator` — create
- `PUT /UtilityOperator/{id}` — update
- `DELETE /UtilityOperator/{id}` — delete

**Database table:** `UtilityOperator`

---

### 3. AssetLink
**EC2:** `t3.micro` | **Path:** `microservices/AssetLink`

Manages asset-to-prosumer associations. Publishes events to Kafka when assets are activated or deactivated.

**Endpoints:**
- `GET /AssetLink` — list all
- `GET /AssetLink/{id}` — get by ID
- `POST /AssetLink` — create and publish `asset-link-events`
- `DELETE /AssetLink/{id}` — delete and publish deactivation event
- `PUT /AssetLink/{id}` — update

**Kafka produced:** `asset-link-events`

**Database table:** `AssetLink`

---

### 4. Telemetry Ingestion
**EC2:** `t3.micro` | **Path:** `microservices/Telemetry`

Ingests real-time telemetry from energy assets. Consumes `asset-link-events` to dynamically create a Kafka consumer thread per asset topic. Validates incoming messages and routes invalid ones to a dead-letter topic.

**Endpoints:**
- `GET /Telemetry` — list all telemetry records
- `GET /Telemetry/{id}` — get by ID
- `POST /Telemetry/Consume` — start consuming a new asset topic (provisions a `DynamicTopicConsumer` thread)

**Kafka consumed:** `asset-link-events` (via SmallRye), per-asset topics (via `DynamicTopicConsumer`)

**Kafka produced:** `telemetry-events`, `invalid-telemetry-events`

**Supported asset types:** `BATTERY`, `SOLAR`, `EV_CHARGER`

**Database table:** `Telemetry`

---

### 5. Flexibility Emission
**EC2:** `t3.micro` | **Path:** `microservices/FlexibilityEmission`

Queries Telemetry for BATTERY asset data and evaluates flexibility opportunities. Publishes events to two topics depending on the outcome.

**Endpoints:**
- `GET /FlexibilityEmission` — list all flexibility events
- `GET /FlexibilityEmission/{id}` — get by ID
- `POST /FlexibilityEmission/analyze` — analyze battery flexibility and publish events

**Logic:** If `soc_percent > 80%` → `SELL`; if `50% < soc < 80%` → `INCENTIVIZE_DISCHARGE`; otherwise → `UNAVAILABLE`. SELL and INCENTIVIZE_DISCHARGE events are also published to `flexibility-offers`.

**Kafka produced:** `flexibility-events`, `flexibility-offers`

**REST dependency:** Telemetry service

**Database table:** `FlexibilityEmission`

---

### 6. Grid Balancing
**EC2:** `t3.micro` | **Path:** `microservices/GridBalancing`

Queries Telemetry for zone-level energy data and generates grid balancing recommendations.

**Endpoints:**
- `GET /GridBalancing` — list all recommendations
- `GET /GridBalancing/{id}` — get by ID
- `POST /GridBalancing/analyze` — analyze grid state and publish recommendation

**Kafka produced:** `balancing-recommendations`

**REST dependency:** Telemetry service

**Database table:** `GridBalancing`

---

### 7. Energy Analytics
**EC2:** `t3.micro` | **Path:** `microservices/EnergyAnalytics`

Aggregates telemetry data and publishes computed energy metrics across 4 Kafka topics.

**Endpoints:**
- `GET /EnergyAnalytics` — list all analytics records
- `GET /EnergyAnalytics/{id}` — get by ID
- `POST /EnergyAnalytics/compute` — compute and publish energy analytics

**Kafka produced:** `energy-discharged-by-zone`, `generated-energy-by-prosumer`, `consumed-energy-by-prosumer`, `average-soc`

**REST dependency:** Telemetry service

**Database table:** `EnergyAnalytics`

---

### 8. Flexibility Forecasting (AI)
**EC2:** Shared with Ollama (`t3.medium`, 20 GB EBS) | **Path:** `microservices/FlexibilityForecasting`

Uses past flexibility events from the database and an LLM (Ollama `llama3.2:1b`) to forecast future flexibility market outcomes. Publishes forecasts to Kafka.

**Endpoints:**
- `GET /FlexibilityForecasting` — list all forecasts
- `GET /FlexibilityForecasting/{id}` — get by ID
- `POST /FlexibilityForecasting/forecast` — run AI forecast and publish result

**AI integration:** Calls local Ollama instance on `localhost:11434` (co-hosted on same EC2). Model: `llama3.2:1b`.

**Kafka produced:** `flexibility-forecasts`

**REST dependency:** FlexibilityEmission service (for past events)

**Database table:** `FlexibilityForecast`

**Note:** FlexibilityForecasting is co-hosted on the Ollama EC2 to stay within the AWS Academy 9-instance limit. It calls Ollama via `localhost:11434`.

---

## Terraform Infrastructure

### Directory Structure

```
Quarkus-Terraform/
├── prosumer/               EC2 + security group for Prosumer
├── utilityoperator/        EC2 + security group for UtilityOperator
├── assetlink/              EC2 + security group for AssetLink
├── telemetry/              EC2 + security group for Telemetry
├── flexibility-emission/   EC2 + security group for FlexibilityEmission
├── grid-balancing/         EC2 + security group for GridBalancing
├── energy-analytics/       EC2 + security group for EnergyAnalytics
├── flexibility-forecasting/ EC2 + security group (unused — co-hosted with Ollama)
└── ollama/                 EC2 + security group for Ollama + FlexibilityForecasting
Kafka/
└── EC2ChangeKafkaConfiguration.tf   Kafka EC2 + topic auto-creation
RDS-Terraform/
└── RDS configuration (MySQL db.t4g.micro)
```

### EC2 Configuration (Quarkus microservices)

| Parameter | Value |
|-----------|-------|
| AMI | `ami-0244b3aa0b9e167c7` (Amazon Linux 2023, x86_64) |
| Instance type | `t3.micro` |
| Root volume | 20 GB (gp3) |
| Security group | Port 8080 (HTTP), Port 22 (SSH) |
| Key pair | `vockey` |

### EC2 Configuration (Ollama)

| Parameter | Value |
|-----------|-------|
| AMI | `ami-0244b3aa0b9e167c7` (Amazon Linux 2023, x86_64) |
| Instance type | `t3.medium` |
| Root volume | 20 GB (gp3) |
| Security group | Port 11434 (Ollama), Port 8080 (FlexibilityForecasting), Port 22 (SSH) |

### RDS Configuration

| Parameter | Value |
|-----------|-------|
| Engine | MySQL 8.0 |
| Instance class | `db.t4g.micro` |
| Database name | `VPPaaS` |
| Username | `teste` |
| Password | `testeteste` |

---

## Deployment

### Prerequisites

- AWS Academy credentials (rotate every ~5 hours via `access.sh`)
- Docker Desktop running locally
- Java 17 (`/Library/Java/JavaVirtualMachines/jdk-17.jdk`)
- Maven (via `./mvnw` wrapper in each microservice)
- Terraform >= 1.0.0
- Docker Hub account

### Configuration — access.sh

Before deploying, update `access.sh` with current AWS Academy credentials:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:"$PATH"

aws_access_key_id=<from AWS Academy lab>
aws_secret_access_key=<from AWS Academy lab>
aws_session_token=<from AWS Academy lab>

yourDockerUsername=<docker hub username>
yourDockerPassword=<docker hub password>
```

### Full Deployment

```bash
cd IntegrationScenarioS3-VPPaaS-2026
./DeploymentAutomation-macOS.sh
```

The script performs the following steps in order:

1. Provisions RDS (MySQL)
2. Provisions Kafka EC2 — auto-creates all 11 topics on boot
3. Builds and deploys: Prosumer, UtilityOperator, Telemetry, AssetLink
4. Captures service addresses
5. Builds and deploys: FlexibilityEmission, GridBalancing, EnergyAnalytics
6. Builds FlexibilityForecasting Docker image (uses `localhost:11434` for Ollama)
7. Deploys Ollama EC2 — installs Ollama, pulls `llama3.2:1b`, starts FlexibilityForecasting container
8. Prints all service endpoints

**Build flag:** `DOCKER_DEFAULT_PLATFORM=linux/amd64` is set during Maven build to ensure x86-compatible images are pushed from Apple Silicon machines.

### Destroy All Resources

```bash
./Destroy.sh
```

Destroys all microservices first, then Ollama, Kafka, and finally RDS.

---

## Parametrization

The following properties are injected automatically by `DeploymentAutomation-macOS.sh` at build time into each service's `application.properties`:

| Property | Injected For | Value Source |
|----------|-------------|-------------|
| `quarkus.datasource.reactive.url` | All services | RDS Terraform output |
| `quarkus.container-image.group` | All services | `access.sh` Docker username |
| `kafka.bootstrap.servers` | All Kafka-enabled services | Kafka Terraform output |
| `quarkus.rest-client.telemetry-service.url` | FlexibilityEmission, GridBalancing, EnergyAnalytics | Telemetry EC2 DNS |
| `quarkus.rest-client.flexibility-emission-service.url` | FlexibilityForecasting | FlexibilityEmission EC2 DNS |
| `quarkus.rest-client.ollama-service.url` | FlexibilityForecasting | `localhost:11434` (co-hosted) |

### Static configuration (per service `application.properties`)

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.username=teste
quarkus.datasource.password=testeteste
quarkus.swagger-ui.always-include=true
quarkus.container-image.build=true
quarkus.container-image.push=true
ollama.model=llama3.2:1b   # FlexibilityForecasting only
```

---

## Key Implementation Changes

### Kafka Alignment
- Renamed topic `grid-balancing-recommendations` → `balancing-recommendations` in GridBalancing microservice to match the design document
- Added `flexibility-offers` topic producer to FlexibilityEmission (publishes SELL and INCENTIVIZE_DISCHARGE events)
- Added `flexibility-forecasts` topic producer to FlexibilityForecasting
- Added `telemetry-events` and `invalid-telemetry-events` producers to Telemetry
- All topics auto-created on Kafka EC2 boot via `Kafka/creation.sh`

### Telemetry — DynamicTopicConsumer Fix
- `DynamicTopicConsumer extends Thread` — `stop()` is final in Java's Thread class; renamed to `shutdown()`
- Emitters (`MutinyEmitter<String>`) cannot be injected into plain threads — injected in `AssetLinkEventConsumer` and `KafkaProvisioningResource` (CDI beans) and passed via constructor
- Added JSON parse error handling: malformed messages route to `invalid-telemetry-events`

### FlexibilityForecasting — EC2 Instance Limit Workaround
- AWS Academy limits accounts to 9 running EC2 instances
- Infrastructure requires: 1 Kafka + 1 Ollama + 8 microservices = 10
- Solution: FlexibilityForecasting runs as a Docker container on the Ollama EC2 (port 8080), calling Ollama on `localhost:11434`
- Ollama EC2 security group exposes both port 11434 and port 8080

### AMI and Instance Type
- Switched from ARM (`t4g`, `ami-0bb7267a511c0a8e8`) to x86 (`t3`, `ami-0244b3aa0b9e167c7`) — AWS Academy blocks Graviton instance types
- Added `root_block_device { volume_size = 20 }` to all EC2s — Amazon Linux 2023 minimal AMI defaults to 2 GB which is insufficient for Docker + application images
- Ollama requires 20 GB for the OS + Docker + Ollama binary + `llama3.2:1b` model (~1.3 GB)

### Ollama systemd Configuration
- Ollama's install script creates a systemd service bound to `127.0.0.1:11434` by default
- Added systemd drop-in override to bind on all interfaces:
  ```
  /etc/systemd/system/ollama.service.d/override.conf
  [Service]
  Environment="OLLAMA_HOST=0.0.0.0"
  ```

### Docker Build
- `DOCKER_DEFAULT_PLATFORM=linux/amd64` set before `./mvnw clean package` so that images built on Apple Silicon (arm64) are pushed as x86_64 and run correctly on `t3` EC2 instances
