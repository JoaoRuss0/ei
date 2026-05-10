package org.acme;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.Blocking;
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
        .flatMap(r -> client.query("CREATE TABLE EnergyAnalytics (type ENUM('ENERGY_DISCHARGED_BY_ZONE', 'ENERGY_GENERATED_BY_PROSUMER', 'ENERGY_CONSUMED_BY_PROSUMER', 'AVERAGE_SOC') NOT NULL, entity_id VARCHAR(255) NOT NULL , value DOUBLE NOT NULL, timestamp DATETIME NOT NULL )").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, entity_id, value, timestamp) VALUES ('ENERGY_DISCHARGED_BY_ZONE', 'LISBON_SOUTH', 34.2, '2020-10-10 20:00')").execute())
        .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (type, entity_id, value, timestamp) VALUES ('ENERGY_GENERATED_BY_PROSUMER', '2', 50, '2020-10-10 20:00')").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<EnergyAnalytics> get() {
        return EnergyAnalytics.findAll(client);
    }
    
    @POST
    @Path("analyse")
    @Blocking
    public Response analyse(AnalyticsRequestDTO dto) {
        Map<Long, Long> assetToProsumer = dto.assets().stream()
                .collect(Collectors.toMap(Asset::id, Asset::prosumerId));

        Map<String, Double> dischargedByZone = dto.events().stream()
                .filter(t -> "BATTERY".equals(t.asset_type()) && t.Current_Output() != null && t.Current_Output() > 0)
                .collect(Collectors.groupingBy(TelemetryEvent::grid_cell_id, Collectors.summingDouble(TelemetryEvent::Current_Output)));

        Map<Long, Double> generatedByProsumer = dto.events().stream()
                .filter(t -> "SOLAR".equals(t.asset_type()) && t.Daily_Total() != null)
                .collect(Collectors.groupingBy(t -> assetToProsumer.get(t.asset_id()), Collectors.summingDouble(TelemetryEvent::Daily_Total)));

        Map<Long, Double> consumedByProsumer = dto.events().stream()
                .filter(t -> "EV_CHARGER".equals(t.asset_type()) && t.Session_Energy() != null)
                .collect(Collectors.groupingBy(t -> assetToProsumer.get(t.asset_id()), Collectors.summingDouble(TelemetryEvent::Session_Energy)));

        Map<String, Double> avgSocByZone = dto.events().stream()
                .filter(t -> "BATTERY".equals(t.asset_type()) && t.State_of_Charge() != null)
                .collect(Collectors.groupingBy(TelemetryEvent::grid_cell_id, Collectors.averagingDouble(TelemetryEvent::State_of_Charge)));

        LocalDateTime now = LocalDateTime.now();

        publishAndSave(EnergyAnalyticsType.ENERGY_DISCHARGED_BY_ZONE, dischargedByZone, discargedEmitter, now);
        publishAndSave(EnergyAnalyticsType.ENERGY_GENERATED_BY_PROSUMER, generatedByProsumer, generatedEmitter, now);
        publishAndSave(EnergyAnalyticsType.ENERGY_CONSUMED_BY_PROSUMER, consumedByProsumer, consumedEmitter, now);
        publishAndSave(EnergyAnalyticsType.AVERAGE_SOC, avgSocByZone, averageSocEmitter, now);

        return Response.ok().build();
    }

    private <K> void publishAndSave(EnergyAnalyticsType type, Map<K, Double> data, Emitter<String> emitter, LocalDateTime ts) {
        data.forEach((k, v) -> {
            EnergyAnalytics event = new EnergyAnalytics(type, String.valueOf(k), v, ts);
            event.save(client).subscribe().with(ok -> {}, err -> System.err.println("Save failed: " + err));
            emitter.send(event.toJson());
        });
    }
}
