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
        .flatMap(r -> client.query("CREATE TABLE EnergyAnalytics (" +
                "id SERIAL PRIMARY KEY," +
                "type ENUM('ENERGY_DISCHARGED_BY_ZONE','ENERGY_GENERATED_BY_PROSUMER','ENERGY_CONSUMED_BY_PROSUMER','AVERAGE_SOC') NOT NULL," +
                "value DOUBLE NOT NULL," +
                "timestamp DATETIME NOT NULL," +
                "prosumer_id BIGINT UNSIGNED," +
                "prosumer_name VARCHAR(255)," +
                "utility_operator_id BIGINT UNSIGNED," +
                "utility_operator_name VARCHAR(255)," +
                "grid_cell_id VARCHAR(255))").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, value, timestamp, grid_cell_id) VALUES ('ENERGY_DISCHARGED_BY_ZONE',  34.2, '2026-04-15 19:00:00', 'PORTO_NORTH')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, value, timestamp, grid_cell_id) VALUES ('ENERGY_DISCHARGED_BY_ZONE',  12.5, '2026-04-15 19:00:00', 'PORTO_SOUTH')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, value, timestamp, prosumer_id, prosumer_name) VALUES ('ENERGY_GENERATED_BY_PROSUMER', 50.0, '2026-04-15 20:00:00', 3, 'Pedro Porto')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, value, timestamp, prosumer_id, prosumer_name) VALUES ('ENERGY_CONSUMED_BY_PROSUMER', 25.0, '2026-04-15 20:00:00', 1, 'Maria Lisbon')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, value, timestamp, grid_cell_id) VALUES ('AVERAGE_SOC', 0.78, '2026-04-15 20:00:00', 'PORTO_NORTH')").execute())
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
                .onItem().invoke(saved -> {
                    if (saved != null && saved > 0 && analytics != null) {
                        for (EnergyAnalytics a : analytics) {
                            emit(a);
                        }
                    }
                })
                .onItem().transform(saved -> Response.ok(Map.of("saved", saved)).build())
                .onFailure().recoverWithItem(err -> Response.serverError().entity(err.getMessage()).build());
    }

    @GET
    @Path("{id}")
    public Uni<Response> getById(@PathParam("id") Long id) {
        return EnergyAnalytics.findById(client, id)
                .onItem().transform(a -> a != null
                        ? Response.ok(a).build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> deleteById(@PathParam("id") Long id) {
        return EnergyAnalytics.delete(client, id)
                .onItem().transform(deleted -> deleted
                        ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    private void emit(EnergyAnalytics a) {
        if (a == null || a.type == null) return;
        String payload = a.toJson();
        switch (a.type) {
            case ENERGY_DISCHARGED_BY_ZONE -> discargedEmitter.send(payload);
            case ENERGY_GENERATED_BY_PROSUMER -> generatedEmitter.send(payload);
            case ENERGY_CONSUMED_BY_PROSUMER -> consumedEmitter.send(payload);
            case AVERAGE_SOC -> averageSocEmitter.send(payload);
        }
    }
}
