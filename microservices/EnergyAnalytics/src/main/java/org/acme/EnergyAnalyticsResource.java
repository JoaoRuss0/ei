package org.acme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("EnergyAnalytics")
public class EnergyAnalyticsResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Channel("energy-discharged-by-zone")
    Emitter<String> discargedEmitter;

    @Channel("generated-energy-by-prosumer")
    Emitter<String> generatedEmitter;

    @Channel("consumed-energy-by-prosumer")
    Emitter<String> consumedEmitter;

    @Channel("average-soc")
    Emitter<String> averageSocEmitter;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate ;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS EnergyAnalytics").execute()
        .flatMap(r -> client.query("CREATE TABLE EnergyAnalytics (type ENUM('ENERGY_DISCHARGED_BY_ZONE', 'ENERGY_GENERATED_BY_PROSUMER', 'ENERGY_CONSUMED_BY_PROSUMER', 'AVERAGE_SOC') NOT NULL," +
                "value DOUBLE NOT NULL, timestamp DATETIME NOT NULL, prosumerId BIGINT UNSIGNED, utilityOperatorId BIGINT UNSIGNED, prosumerName VARCHAR(255), utilityOperatorName VARCHAR(255), gridCellId BIGINT UNSIGNED)").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, entity_id, value, timestamp) VALUES ('ENERGY_DISCHARGED_BY_ZONE', 'LISBON_SOUTH', 34.2, '2020-10-10 20:00')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, entity_id, value, timestamp) VALUES ('ENERGY_GENERATED_BY_PROSUMER', '2', 50, '2020-10-10 20:00')").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<EnergyAnalytics> get() {
        return EnergyAnalytics.findAll(client);
    }

    @POST
    @Path("save")
    public Uni<Response> saveAll(List<EnergyAnalytics> analytics) {
        return EnergyAnalytics.saveAll(client, analytics)
                .onItem().transform(saved -> Response.ok(Map.of("saved", saved)).build())
                .onFailure().recoverWithItem(err -> Response.serverError().entity(err.getMessage()).build());
    }
}
