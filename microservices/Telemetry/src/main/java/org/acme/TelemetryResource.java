package org.acme;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import org.acme.model.Topic;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;

import java.util.List;

@Path("Telemetry")
public class TelemetryResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @Inject
    KafkaConsumerService service;

    // Topics for the seeded AssetLinks in the AssetLink service.
    // Keep these in sync with AssetLinkResource.SEED_TOPICS — they share the
    // {assetLinkId}-{utilityOperatorName} naming used by KafkaTopicService.
    static final List<String> SEED_TOPICS = List.of(
            "1-ArcoCegoLisbon",
            "2-PracadeBocage",
            "3-PracadaBoavista",
            "4-PracaDomFranciscoGomes",
            "5-PracadaBoavista"
    );

    @Inject
    @ConfigProperty(name = "myapp.seed.consume-topics", defaultValue = "true")
    boolean seedConsumeTopics;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
            if (seedConsumeTopics) {
                consumeSeedTopics();
            }
        }
    }

    private void consumeSeedTopics() {
        for (String topicName : SEED_TOPICS) {
            service.consume(new Topic(topicName));
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS Telemetry").execute()
                .flatMap(r -> client.query("CREATE TABLE Telemetry (id SERIAL PRIMARY KEY,   "
                        + " timeStamp DATETIME NOT NULL, "
                        + " asset_id BIGINT UNSIGNED NOT NULL, "
                        + " grid_cell_id VARCHAR(255) NOT NULL, "
                        + " asset_type VARCHAR(255) NOT NULL,  "
                        + " State_of_Charge	FLOAT, "
                        + " Available_Energy FLOAT, "
                        + " Current_Output	FLOAT, "
                        + " Max_Capacity	FLOAT, "
                        + " State_of_Health	FLOAT, "
                        + " Status VARCHAR(255), "
                        + " Current_Generation FLOAT, "
                        + " Daily_Total FLOAT, "
                        + " Grid_Voltage FLOAT, "
                        + " Frequency FLOAT, "
                        + " Plug_Status VARCHAR(255), "
                        + " Charging_Rate FLOAT, "
                        + " Session_Energy FLOAT, "
                        + " EV_SoC FLOAT)").execute())
                .flatMap(r -> client.query("DROP TABLE IF EXISTS TopicSubscription").execute())
                .flatMap(r -> client.query("CREATE TABLE TopicSubscription (topic_name VARCHAR(255) PRIMARY KEY, owner_service VARCHAR(255) NOT NULL)").execute())
                // Historical telemetry tied to the seeded FlexibilityEvents — gives
                // FlexibilityForecasting concrete before/after rows per event.
                //   Event 1 (asset 6 SELL 04-15 19:30): battery discharges after the signal
                //   Event 2 (asset 3 SELL 04-20 19:30): battery discharges after the signal
                //   Event 3 (asset 1 UNAVAILABLE 04-22 03:00): battery offline / very low SoC
                //   Event 4 (asset 9 UNAVAILABLE 04-25 12:00): EV unplugged
                .flatMap(r -> client.query("INSERT INTO Telemetry (timeStamp, asset_id, grid_cell_id, asset_type, State_of_Charge, Current_Output, Status, Current_Generation, Daily_Total, Plug_Status, Charging_Rate, Session_Energy) VALUES "
                        + "('2026-04-15 19:25:00', 6, 'PORTO_NORTH',    'BATTERY',    0.92,   0.0, 'ONLINE',   NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-04-15 19:35:00', 6, 'PORTO_NORTH',    'BATTERY',    0.85,  15.0, 'ONLINE',   NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-04-20 19:25:00', 3, 'SETUBAL_CENTRO', 'BATTERY',    0.88,   0.0, 'ONLINE',   NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-04-20 19:35:00', 3, 'SETUBAL_CENTRO', 'BATTERY',    0.82,  12.0, 'ONLINE',   NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-04-22 03:00:00', 1, 'LISBON_NORTH',   'BATTERY',    0.20,   0.0, 'OFFLINE',  NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-04-25 12:00:00', 9, 'FARO_CENTRO',    'EV_CHARGER', NULL,   NULL, 'OK',      NULL, NULL, 'UNPLUGGED', NULL, NULL)").execute())
                // Current snapshot @ 2026-05-30 19:00 — peak hour in Porto/Setubal.
                // PORTO_NORTH overloaded (+80 EV − 8 solar = +72 vs max_load 50) → run
                // GridBalancingRecommendation BPMN to get a transfer recommendation.
                // Asset 6 SoC=0.92 during peak → run FlexibilityEmission BPMN to emit SELL.
                .flatMap(r -> client.query("INSERT INTO Telemetry (timeStamp, asset_id, grid_cell_id, asset_type, State_of_Charge, Current_Output, Status, Current_Generation, Daily_Total, Plug_Status, Charging_Rate, Session_Energy) VALUES "
                        + "('2026-05-30 19:00:00', 1, 'LISBON_NORTH',   'BATTERY',    0.65,   0.0, 'ONLINE',  NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 2, 'LISBON_SOUTH',   'SOLAR',      NULL,   NULL, NULL,      5.0, 42.0, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 3, 'SETUBAL_CENTRO', 'BATTERY',    0.85,  10.0, 'ONLINE',  NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 4, 'SETUBAL_CENTRO', 'EV_CHARGER', NULL,   NULL, 'OK',     NULL, NULL, 'CHARGING',  22.0, 18.0),"
                        + "('2026-05-30 19:00:00', 5, 'PORTO_NORTH',    'SOLAR',      NULL,   NULL, NULL,      8.0, 65.0, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 6, 'PORTO_NORTH',    'BATTERY',    0.92,   0.0, 'ONLINE',  NULL, NULL, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 7, 'PORTO_NORTH',    'EV_CHARGER', NULL,   NULL, 'OK',     NULL, NULL, 'CHARGING',  80.0, 55.0),"
                        + "('2026-05-30 19:00:00', 8, 'FARO_CENTRO',    'SOLAR',      NULL,   NULL, NULL,      6.0, 55.0, NULL,        NULL, NULL),"
                        + "('2026-05-30 19:00:00', 9, 'FARO_CENTRO',    'EV_CHARGER', NULL,   NULL, 'OK',     NULL, NULL, 'PLUGGED',   NULL, 12.0)").execute())
                .await().indefinitely();
    }

    @POST
    @Path("consume")
    @Blocking
    public String ProvisioningConsumer(Topic topic) {
        service.consume(topic);
        return "New worker started";
    }

    @POST
    @Path("stop")
    @Blocking
    public void stop(Topic topic) {
        service.stop(topic);
    }

    @GET
    public Multi<Telemetry> get() {
        return Telemetry.findAll(client);
    }

    @GET
    @Path("by-asset/{assetId}")
    public Multi<Telemetry> getByAssetId(@PathParam("assetId") Long assetId) {
        return Telemetry.findByAssetId(client, assetId);
    }

    @GET
    @Path("by-grid-cell/{gridCellId}")
    public Multi<Telemetry> getByGridCellId(@PathParam("gridCellId") String gridCellId) {
        return Telemetry.findByGridCellId(client, gridCellId);
    }

    @GET
    @Path("by-grid-cell-ids/")
    public Multi<Telemetry> getByGridCellId(@QueryParam("gridCellIds") List<String> gridCellIds) {
        return Telemetry.findByGridCellIds(client, gridCellIds);
    }

    @GET
    @Path("by-asset-ids/")
    public Multi<Telemetry> getByAssetIds(@QueryParam("assetIds") List<Long> assetIds) {
        return Telemetry.findByAssetIds(client, assetIds);
    }
}

