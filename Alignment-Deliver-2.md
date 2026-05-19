# Deliverable 2 Alignment

This document is aligned with `ProjectDescription.pdf`, section 8, "2ND SPRINT".

Deliverable 2 must reuse the Sprint 1 microservices and finalize the business processes using:

- Camunda for process orchestration.
- Kong as the API gateway between Camunda and the microservices.
- The existing Quarkus microservices, Kafka, RDS, and Ollama integrations.
- BPMN files, Terraform scripts, installation procedures, parametrization notes, and end-to-end test evidence.

The deadline stated in the project description is `7/6/2026 23h59m`.

## Current Status

The new additions help, but they are not deliverable-ready yet.

What exists now:

- `BPMN/ProsumerManagement.bpmn`
- `BPMN/UtilityOperatorManagement.bpmn`
- `BPMN/AssetLinkManagement.bpmn`
- `BPMN/forms/*.form`
- `BPMN/Generic-BPMN-Patterns-For-Your-Reuse/*`
- `Camunda-Terraform/`
- `KongTerraform/`
- `KongaTerraform/`

Main issues:

- The Terraform folders are at the repository root instead of under `terraform/`, unlike every other deployed component.
- Root `deploy.sh`, `undeploy.sh`, and `addresses.sh` do not deploy or report Kong, Konga, or Camunda.
- Kong starts, but no service or route is registered for the microservices.
- Existing BPMNs still call hardcoded EC2 microservice URLs instead of going through Kong.
- The AssetLink BPMN starts telemetry consumption but does not create the Kafka topic through the AssetLink microservice first.
- The AssetLink BPMN does not currently collect the `assetId` and `gridCellId` needed by the implemented topic endpoint.
- Several required Sprint 2 business processes are still missing as BPMN artifacts.
- Grid Balancing has a code/API path mismatch: the resource class is named as Grid Balancing, but it exposes `@Path("AssetLink")`.
- `GridCell/by-ids` currently accepts `List<Long>` even though grid-cell IDs are strings such as `PORTO_NORTH`.

## Required Sprint 2 Process Coverage

| Required by PDF | Current artifact | Status | Required action |
| --- | --- | --- | --- |
| Prosumer Management | `BPMN/ProsumerManagement.bpmn` | Partial | Keep it, but replace direct EC2 calls with `kongUrl`. |
| Utility Operator Management | `BPMN/UtilityOperatorManagement.bpmn` | Partial | Keep it, but replace direct EC2 calls with `kongUrl`. Document how Grid Cell lifecycle is covered, because the PDF says Utility Operator Management manages grid zones. |
| Asset Link Management | `BPMN/AssetLinkManagement.bpmn` | Partial | Keep it, but route through Kong, create Kafka topics, start telemetry consumption with the correct topic payload, and include offboarding/deletion evidence. |
| Telemetry Ingestion | Embedded in `AssetLinkManagement.bpmn` as `TelemetryManagement` | Weak coverage | Prefer creating `BPMN/TelemetryIngestion.bpmn`. If kept embedded, document it explicitly as the Sprint 2 Telemetry Ingestion process. |
| Flexibility Emission | Missing | Missing | Create `BPMN/FlexibilityEmission.bpmn`. |
| Grid Balancing Recommendation | Missing | Missing | Create `BPMN/GridBalancingRecommendation.bpmn` and fix the REST path mismatch. |
| Energy Analytics | Missing | Missing | Create `BPMN/EnergyAnalytics.bpmn`. |
| Flexibility Forecasting (AI) | Missing | Missing | Create `BPMN/FlexibilityForecasting.bpmn` using Ollama. |

The generic BPMN pattern files are useful references, but they should not be counted as project business-process deliverables.

## Terraform Alignment

### Step 1 - Move the new Terraform modules under `terraform/`

The project already organizes infrastructure under `terraform/`. Move the three new root-level folders:

```text
KongTerraform/       -> terraform/kong/
KongaTerraform/      -> terraform/konga/
Camunda-Terraform/   -> terraform/camunda/
```

Expected structure:

```text
terraform/kong/
  EC2Kong.tf
  deploy.sh

terraform/konga/
  EC2Konga.tf
  deploy.sh

terraform/camunda/
  EC2CamundaEngine.tf
  deploy.sh
```

### Step 2 - Add AWS credential configuration

Because these modules will live one level below `terraform/`, use the same credential pattern as `terraform/kafka` and `terraform/rds`:

```hcl
provider "aws" {
  shared_credentials_files = ["../.aws/credentials"]
  profile                  = "account_1"
  region                   = "us-east-1"
}
```

Apply this to:

- `terraform/kong/EC2Kong.tf`
- `terraform/konga/EC2Konga.tf`
- `terraform/camunda/EC2CamundaEngine.tf`

### Step 3 - Add Camunda output

`terraform/camunda/EC2CamundaEngine.tf` should output the Camunda public DNS:

```hcl
output "address" {
  value       = aws_instance.exampleInstallCamundaEngine.public_dns
  description = "Connect to Camunda at this endpoint"
}
```

Kong and Konga already have equivalent outputs.

### Step 4 - Fix Camunda startup

Current Camunda startup is broken:

```bash
sudo runuser -l ec2-user -c 'cd /c8run && ./start.sh'
```

The archive is extracted into the current working directory, not `/c8run`. Use a real path and run detached:

```bash
cd /root/c8run && ./start.sh --detached
```

Also document the relevant Camunda URLs in the report/install notes, for example:

- `http://<camunda-dns>:8080`
- `http://<camunda-dns>:8080/operate`
- `http://<camunda-dns>:8080/tasklist`

### Step 5 - Register Kong services and routes

Starting Kong is not enough for Deliverable 2. Camunda must call Kong, and Kong must forward to the correct microservice.

Register all microservices:

| Public Kong path | Upstream service path |
| --- | --- |
| `/Prosumer` | `http://<prosumer-dns>:8080/Prosumer` |
| `/UtilityOperator` | `http://<utility-operator-dns>:8080/UtilityOperator` |
| `/AssetLink` | `http://<asset-link-dns>:8080/AssetLink` |
| `/Telemetry` | `http://<telemetry-dns>:8080/Telemetry` |
| `/FlexibilityEmission` | `http://<flexibility-emission-dns>:8080/FlexibilityEmission` |
| `/GridBalancingRecommendation` | `http://<grid-balancing-dns>:8080/GridBalancingRecommendation` |
| `/EnergyAnalytics` | `http://<energy-analytics-dns>:8080/EnergyAnalytics` |
| `/Asset` | `http://<asset-dns>:8080/Asset` |
| `/GridCell` | `http://<grid-cell-dns>:8080/GridCell` |

Important: the current Grid Balancing code exposes `@Path("AssetLink")`. Fix it to `@Path("GridBalancingRecommendation")` before registering the route above. Otherwise Kong has to special-case Grid Balancing and it will collide conceptually with the real AssetLink service.

Do not rely on local shell environment variables automatically existing inside EC2 user-data. Inject the microservice DNS values into the Kong `deploy.sh` by either:

- using Terraform `templatefile(...)`, or
- following the existing root `deploy.sh` pattern and rewriting/appending the generated Kong registration commands before `terraform apply`.

Registration logic should wait for the Admin API first:

```bash
until curl -fsS http://localhost:8001/status >/dev/null; do
  sleep 2
done
```

Then create one service and one route per microservice using Kong Admin API port `8001`.

### Step 6 - Integrate Kong, Konga, and Camunda in root scripts

Update root `deploy.sh`:

- Deploy RDS, Kafka, and Ollama as today.
- Deploy all microservices as today.
- Read all microservice public DNS values.
- Deploy Kong after the microservices, because Kong route registration needs their DNS values.
- Deploy Konga after Kong, or document the manual Konga connection to `http://<kong-dns>:8001`.
- Deploy Camunda and print the Camunda endpoint.
- Print the final `kongUrl`, for example `http://<kong-dns>:8000`.
- Print the final `ollamaUrl`, for example `http://<ollama-dns>:11434`.

Update root `undeploy.sh`:

- Destroy `terraform/kong`.
- Destroy `terraform/konga`.
- Destroy `terraform/camunda`.

Update `addresses.sh`:

- Print Kong proxy URL.
- Print Kong Admin URL.
- Print Konga URL.
- Print Camunda URL.

## BPMN Alignment

### Global BPMN rule

All microservice calls from BPMN should go through:

```text
kongUrl = http://<kong-dns>:8000
```

Use FEEL expressions for connector URLs:

```text
= kongUrl + "/Prosumer"
= kongUrl + "/Telemetry/consume"
```

Internal Camunda REST calls such as `http://localhost:8080/v2/messages/publication` may remain local if the connector runtime executes on the Camunda host. If connectors are moved outside the Camunda host, parameterize those URLs too with `camundaRestUrl`.

### Existing BPMNs to fix

#### `BPMN/ProsumerManagement.bpmn`

Replace:

```text
http://ec2-54-224-51-23.compute-1.amazonaws.com:8080/Prosumer
```

with:

```text
= kongUrl + "/Prosumer"
```

#### `BPMN/UtilityOperatorManagement.bpmn`

Replace:

```text
http://ec2-34-207-249-215.compute-1.amazonaws.com:8080/UtilityOperator
```

with:

```text
= kongUrl + "/UtilityOperator"
```

#### `BPMN/AssetLinkManagement.bpmn`

Replace all direct EC2 service calls:

```text
http://ec2-34-238-164-142.compute-1.amazonaws.com:8080/Prosumer
= "http://ec2-54-159-91-220.compute-1.amazonaws.com:8080/AssetLink/" + prosumerID + "/" + UtilityOperatorID
http://ec2-54-159-91-220.compute-1.amazonaws.com:8080/AssetLink
http://ec2-100-30-178-12.compute-1.amazonaws.com:8080/UtilityOperator
= "http://ec2-100-30-178-12.compute-1.amazonaws.com:8080/UtilityOperator/" + UtilityOperatorID
http://ec2-23-22-152-18.compute-1.amazonaws.com:8080/Telemetry/Consume
```

with Kong URLs:

```text
= kongUrl + "/Prosumer"
= kongUrl + "/AssetLink/" + prosumerID + "/" + UtilityOperatorID
= kongUrl + "/AssetLink"
= kongUrl + "/UtilityOperator"
= kongUrl + "/UtilityOperator/" + UtilityOperatorID
= kongUrl + "/Telemetry/consume"
```

Case matters. The implemented Telemetry endpoint is:

```java
@Path("Telemetry")
@Path("consume")
```

So the BPMN URL must be `/Telemetry/consume`, not `/Telemetry/Consume`.

### AssetLink topic creation gap

The AssetLink BPMN should create the Kafka topic before asking Telemetry to consume it.

First make the topic naming convention consistent. The PDF mentions both `AssetID-ZoneID` and `AssetLinkID-UtilityOperator` in different places. The current code and Sprint 1 diagrams use `assetId-gridCellId`, so the lowest-risk path is to align BPMN and documentation to the current implementation:

```text
topicName = assetId + "-" + gridCellId
```

The current AssetLink BPMN only works with `prosumerID` and `UtilityOperatorID`; it does not identify a specific asset or grid cell. Add one of these before topic creation:

- a user task where the user selects `assetId` and `gridCellId`, or
- service tasks that read the prosumer assets and operator grid cells:

```text
GET = kongUrl + "/Asset/active/by-prosumer/" + prosumerID
GET = kongUrl + "/GridCell/by-operator-ids?operatorIds=" + UtilityOperatorID
```

Then add a service task after the AssetLink is created and the required `assetId` and `gridCellId` are known:

```text
POST = kongUrl + "/AssetLink/topic/" + assetId + "/" + gridCellId
```

Then start Telemetry consumption:

```text
POST = kongUrl + "/Telemetry/consume"
```

The body should match `microservices/Telemetry/src/main/java/org/acme/model/Topic.java`:

```json
{
  "topicName": "= string(assetId) + \"-\" + gridCellId"
}
```

The current BPMN body uses `TopicName` and builds `AssetLinkID-UtilityOperator`. That does not match the current AssetLink topic service, which creates topics as:

```text
assetId-gridCellId
```

For decommissioning/offboarding tests, also cover:

```text
POST   = kongUrl + "/Telemetry/stop"
DELETE = kongUrl + "/AssetLink/topic/" + assetId + "/" + gridCellId
DELETE = kongUrl + "/AssetLink/" + assetLinkId
```

This aligns with the project description's warning about avoiding logically active "phantom" assets.

## Supporting Microservice API Fixes

These are not new Sprint 2 features, but the BPMN processes above will fail without them.

### Grid Balancing path

Change:

```java
// microservices/GridBalancingRecommendation/src/main/java/org/acme/GrigBalancingRecommendationResource.java
@Path("AssetLink")
```

to:

```java
@Path("GridBalancingRecommendation")
```

Then expose it in Kong as:

```text
GET  /GridBalancingRecommendation
POST /GridBalancingRecommendation/balance
```

### GridCell `by-ids` type

Current code declares:

```java
public Multi<GridCell> getByCellIds(@QueryParam("ids") List<Long> ids)
public static Multi<GridCell> findByIds(MySQLPool client, List<Long> ids)
```

But `GridCell.id` is a string (`PORTO_NORTH`, `LISBON_SOUTH`, etc.). Change both methods to `List<String>` and bind parameters with `addString`, otherwise `GET /GridCell/by-ids?ids=PORTO_NORTH` will not work for Flexibility Emission.

## Missing BPMN Processes

### `BPMN/TelemetryIngestion.bpmn`

Create this as a standalone BPMN or explicitly document the existing embedded `TelemetryManagement` process as the Telemetry Ingestion deliverable.

Minimum flow:

1. Receive or calculate `assetId` and `gridCellId`.
2. Create the Kafka topic through AssetLink:

```text
POST = kongUrl + "/AssetLink/topic/" + assetId + "/" + gridCellId
```

3. Start the Telemetry consumer:

```text
POST = kongUrl + "/Telemetry/consume"
body = { "topicName": string(assetId) + "-" + gridCellId }
```

4. Use the event producer to send telemetry to the topic.
5. Verify stored telemetry:

```text
GET = kongUrl + "/Telemetry"
```

### `BPMN/FlexibilityEmission.bpmn`

Match `diagrams/08_flexibility_emission.puml` and the current implemented endpoints.

Expected flow:

1. Read the prosumer:

```text
GET = kongUrl + "/Prosumer/" + prosumerId
```

2. Read active battery assets for the prosumer:

```text
GET = kongUrl + "/Asset/active/by-prosumer/" + prosumerId
```

3. Read latest telemetry for those asset/grid-cell pairs:

```text
POST = kongUrl + "/Telemetry/latest"
body = [
  { "assetId": 1, "gridCellId": "PORTO_NORTH" }
]
```

4. Read grid-cell data. This requires the `GridCell/by-ids` type fix described above:

```text
GET = kongUrl + "/GridCell/by-ids?ids=" + gridCellIds
```

5. Evaluate flexibility:

```text
POST = kongUrl + "/FlexibilityEmission/evaluate"
body = {
  "prosumer_id": prosumerId,
  "events": telemetryEvents,
  "cells": gridCells
}
```

Expected evidence:

- The process completes in Camunda.
- A `FlexibilityEvent` row is stored when rules match.
- A message is produced to Kafka topic `flexibility-offers`.

### `BPMN/GridBalancingRecommendation.bpmn`

First fix:

```java
// microservices/GridBalancingRecommendation/src/main/java/org/acme/GrigBalancingRecommendationResource.java
@Path("GridBalancingRecommendation")
```

Then use this process flow, aligned with `diagrams/09_grid_balancing_recommendation.puml`:

1. Read all grid cells:

```text
GET = kongUrl + "/GridCell"
```

2. Read active assets for those grid cells:

```text
GET = kongUrl + "/Asset/active/by-grid-cell-ids?cellIds=" + gridCellIds
```

3. Read latest telemetry:

```text
POST = kongUrl + "/Telemetry/latest"
body = [
  { "assetId": 1, "gridCellId": "PORTO_NORTH" }
]
```

4. Produce balancing recommendation:

```text
POST = kongUrl + "/GridBalancingRecommendation/balance"
body = {
  "events": telemetryEvents,
  "cells": gridCells
}
```

Expected evidence:

- The process completes in Camunda.
- A `GridBalancingRecommendation` row is stored.
- A message is produced to Kafka topic `balancing-recommendation`.

### `BPMN/EnergyAnalytics.bpmn`

Match `diagrams/10_energy_analytics.puml` and the implemented API.

Expected flow:

1. Read all assets:

```text
GET = kongUrl + "/Asset"
```

2. Read recent telemetry:

```text
GET = kongUrl + "/Telemetry/last-hour"
```

3. Run analytics:

```text
POST = kongUrl + "/EnergyAnalytics/analyse"
body = {
  "events": telemetryEvents,
  "assets": assets
}
```

The process does not need to read all Prosumers if the Asset records already carry the `prosumer_id` mapping.

Expected evidence:

- The process completes in Camunda.
- Analytics rows are stored.
- Messages are produced to:
  - `energy-discharged-by-zone`
  - `generated-energy-by-prosumer`
  - `consumed-energy-by-prosumer`
  - `average-soc`

### `BPMN/FlexibilityForecasting.bpmn`

This is the AI process required by the PDF. It should use Ollama directly or through a documented API gateway route.

Recommended flow:

1. Read past flexibility events:

```text
GET = kongUrl + "/FlexibilityEmission"
```

2. Optionally read supporting telemetry around a selected asset event:

```text
GET = kongUrl + "/Telemetry/asset/" + assetId + "/around/" + timestamp
```

3. Send a prompt to Ollama:

```text
POST = ollamaUrl + "/api/generate"
```

Example body shape:

```json
{
  "model": "llama3.2",
  "prompt": "Analyze these VPPaaS flexibility events and summarize success rate, risk, and sentiment: ...",
  "stream": false
}
```

4. Store the result as a process variable and show it in a user task or final process output.

Expected evidence:

- Camunda process instance completes.
- Ollama returns a response.
- The report shows the prompt, response, and how it supports flexibility forecasting.

## Deployment & Validation Workflow

After all code and file changes have been applied, follow this iterative process to verify the implementation end-to-end.

### Round 1 — Infrastructure and microservices

1. Ensure `access.sh` exports valid `DockerUsername` and `DockerPassword` and is sourced:

```bash
source access.sh
```

2. Run the root deploy script:

```bash
./deploy.sh
```

3. Once it completes, run:

```bash
./addresses.sh
```

4. Share the output — specifically the public DNS for:
   - Each microservice (Prosumer, UtilityOperator, AssetLink, Telemetry, FlexibilityEmission, GridBalancingRecommendation, EnergyAnalytics, Asset, GridCell)
   - Kong proxy (`http://<kong-dns>:8000`)
   - Kong admin (`http://<kong-dns>:8001`)
   - Konga (`http://<konga-dns>:1337`)
   - Camunda (`http://<camunda-dns>:8080`)
   - Ollama (`http://<ollama-dns>:11434`)

5. Smoke-test Kong routes:

```bash
curl http://<kong-dns>:8000/Prosumer
curl http://<kong-dns>:8000/UtilityOperator
# repeat for all registered services
```

Share any errors or unexpected responses so Kong route registration can be fixed.

### Round 2 — BPMN deployment and first process runs

1. Open Camunda at `http://<camunda-dns>:8080`.
2. Deploy all BPMN files and forms from the `BPMN/` folder via the Modeler or Operate UI.
3. Set the required process variables before starting any process:
   - `kongUrl` = `http://<kong-dns>:8000`
   - `ollamaUrl` = `http://<ollama-dns>:11434`
   - `camundaRestUrl` = `http://localhost:8080` (if running connectors on the Camunda host)
4. Start each process in order and note any failures:
   - `ProsumerManagement`
   - `UtilityOperatorManagement`
   - `AssetLinkManagement`
   - `TelemetryIngestion`
   - `FlexibilityEmission`
   - `GridBalancingRecommendation`
   - `EnergyAnalytics`
   - `FlexibilityForecasting`
5. For each failure, share the error message from Camunda Operate (connector error, HTTP status, FEEL expression error, or timeout). The BPMN connector bindings or URL expressions will be fixed based on the actual runtime errors.

### Round 3 — End-to-end validation

Once all processes complete without connector errors, run the full test checklist from the section below. For each test, share:
- The exact command or Camunda screenshot
- The Kong route output (`curl` response)
- Kafka consumer output (if applicable)
- Database result (if applicable)

Any remaining failures will be fixed in this round before the final ZIP is produced.

---

## Documentation Required for Deliverable 2

The final ZIP must include the report PDF plus code, scripts, and artifacts. The report should include:

- Updated architecture showing Camunda -> Kong -> microservices.
- List of BPMN files and what process each one implements.
- List of forms used by each BPMN.
- Kong route table.
- Terraform module structure.
- Install/deploy procedure:
  - `access.sh` setup.
  - root `deploy.sh`.
  - BPMN/form deployment into Camunda.
  - required process variables: `kongUrl`, `ollamaUrl`, and optionally `camundaRestUrl`.
- End-to-end test documentation.
- Parametrization:
  - AWS region/profile.
  - Docker Hub credentials.
  - Kafka bootstrap servers.
  - RDS URL.
  - Kong proxy/admin URLs.
  - Camunda URL.
  - Ollama URL/model.

## End-to-End Test Checklist

Document these as evidence, preferably with the exact command, Camunda screenshot, Kong route output, Kafka consumer output, and database result.

| Test | Expected evidence |
| --- | --- |
| Deployment smoke test | RDS, Kafka, microservices, Kong, Konga, Camunda, and Ollama addresses printed by `deploy.sh` or `addresses.sh`. |
| Kong route test | `curl http://<kong-dns>:8000/Prosumer` and equivalent checks for all registered services. |
| Prosumer Management | Camunda process completes and `GET /Prosumer` through Kong shows the created prosumer. |
| Utility Operator Management | Camunda process completes and `GET /UtilityOperator` through Kong shows the created operator. |
| Asset Link Management | Camunda creates an asset link, creates the Kafka topic, and starts Telemetry consumption. |
| Telemetry Ingestion | Event producer publishes telemetry and `GET /Telemetry` through Kong shows stored events. |
| Asset Link decommissioning | Telemetry consumer stops, Kafka topic is deleted, and AssetLink is removed. |
| Flexibility Emission | Matching telemetry creates a flexibility event and Kafka message in `flexibility-offers`. |
| Grid Balancing Recommendation | Overloaded cell data creates a recommendation and Kafka message in `balancing-recommendation`. |
| Energy Analytics | Analytics process writes metrics and produces the four analytics Kafka topic messages. |
| Flexibility Forecasting AI | Camunda calls Ollama and records the generated forecast/sentiment result. |

## Summary of Required Changes

| Item | Action | Priority |
| --- | --- | --- |
| `KongTerraform/` | Move to `terraform/kong/` and integrate with root scripts. | High |
| `KongaTerraform/` | Move to `terraform/konga/` and integrate with root scripts. | High |
| `Camunda-Terraform/` | Move to `terraform/camunda/`, add credentials/output, fix startup path. | High |
| `deploy.sh` | Deploy Kong/Konga/Camunda after infrastructure and microservices. | High |
| `undeploy.sh` | Destroy Kong/Konga/Camunda modules. | Medium |
| `addresses.sh` | Print Kong, Konga, and Camunda URLs. | Medium |
| Kong deploy script | Register routes for all microservices. | High |
| Grid Balancing resource path | Change `@Path("AssetLink")` to `@Path("GridBalancingRecommendation")`. | High |
| Existing BPMNs | Replace hardcoded EC2 URLs with `kongUrl`. | High |
| `AssetLinkManagement.bpmn` | Add Kafka topic creation before `/Telemetry/consume`; fix topic body and URL case. | High |
| `TelemetryIngestion.bpmn` | Create standalone BPMN or explicitly document embedded `TelemetryManagement`. | Medium |
| `FlexibilityEmission.bpmn` | Create. | High |
| `GridBalancingRecommendation.bpmn` | Create. | High |
| `EnergyAnalytics.bpmn` | Create. | High |
| `FlexibilityForecasting.bpmn` | Create with Ollama call. | High |
| Report/test docs | Add Sprint 2 end-to-end process execution evidence. | High |
