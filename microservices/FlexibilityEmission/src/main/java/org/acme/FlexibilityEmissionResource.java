package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Path("FlexibilityEmission")
public class FlexibilityEmissionResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Channel("flexibility-offers")
    MutinyEmitter<String> offerEmitter;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") 
    boolean schemaCreate;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS FlexibilityEvent").execute()
        .flatMap(r -> client.query("CREATE TABLE FlexibilityEvent (id SERIAL PRIMARY KEY, asset_id BIGINT UNSIGNED NOT NULL, prosumer_id BIGINT UNSIGNED NOT NULL, event_type VARCHAR(255) NOT NULL, event_time DATETIME NOT NULL , FOREIGN KEY (prosumer_id) REFERENCES Prosumer(id), FOREIGN KEY (asset_id) REFERENCES Asset(id))").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (asset_id, prosumer_id, event_type, event_time) VALUES ('asset-1', 1, 'UNAVAILABLE', '2020-10-10 20:00')").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (asset_id, prosumer_id, event_type, event_time) VALUES ('asset-2', 1, 'SELL', '2020-10-10 21:00')").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<FlexibilityEvent> get() {
        return FlexibilityEvent.findAll(client);
    }
    
    @POST
    @Path("/evaluate")
    @Blocking
    public Response evaluate(EvaluateRequestDTO dto) {

        record PeakHoursInterval(LocalDateTime start, LocalDateTime end){}
        HashMap<String, PeakHoursInterval> peakHoursPerCell = new HashMap<>();
        dto.cells().forEach(data -> peakHoursPerCell.put(data.id(), new PeakHoursInterval(data.peakHoursStartTime(), data.peakHoursEndTime())));

        for (TelemetryEvent event : dto.events()) {
            if (!event.asset_type().equals("BATTERY")) continue;
            PeakHoursInterval interval = peakHoursPerCell.get(event.grid_cell_id());

            if (event.timeStamp().isAfter(interval.start) && event.timeStamp().isBefore(interval.end) && event.State_of_Charge() > 0.9f) {
                FlexibilityEvent newEvent = new FlexibilityEvent(event.asset_id(), dto.prosumer_id(), FlexibilityEventType.SELL);
                boolean result = newEvent.save(client).await().indefinitely();
                if (!result) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

                offerEmitter.send(newEvent.toJson());
                continue;
            }

            if (event.State_of_Charge() >= 0.2f) continue;

            FlexibilityEvent newEvent = new FlexibilityEvent(event.asset_id(), dto.prosumer_id(), FlexibilityEventType.UNAVAILABLE_FOR_BALANCING);
            boolean result = newEvent.save(client).await().indefinitely();
            if (!result) return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            offerEmitter.send(newEvent.toString());
        }

        return Response.ok().build();
    }
}
