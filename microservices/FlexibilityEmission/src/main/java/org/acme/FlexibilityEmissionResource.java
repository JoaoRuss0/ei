package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
    Emitter<String> offerEmitter;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("DROP TABLE IF EXISTS FlexibilityEvent").execute()
        .flatMap(r -> client.query("CREATE TABLE FlexibilityEvent (id SERIAL PRIMARY KEY, asset_id BIGINT UNSIGNED NOT NULL, prosumer_id BIGINT UNSIGNED NOT NULL, event_type VARCHAR(255) NOT NULL, event_time DATETIME NOT NULL)").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (1, 6, 3, 'SELL',                     '2026-04-15 19:30:00')").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (2, 3, 2, 'SELL',                     '2026-04-20 19:30:00')").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (3, 1, 1, 'UNAVAILABLE_FOR_BALANCING','2026-04-22 03:00:00')").execute())
        .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (4, 9, 4, 'UNAVAILABLE_FOR_BALANCING','2026-04-25 12:00:00')").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<FlexibilityEvent> get() {
        return FlexibilityEvent.findAll(client);
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getById(@PathParam("id") Long id) {
        return FlexibilityEvent.findById(client, id)
                .onItem().transform(event -> event != null
                        ? Response.ok(event).build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/by-prosumer/{prosumerId}")
    public Multi<FlexibilityEvent> getByProsumerId(@PathParam("prosumerId") Long prosumerId) {
        return FlexibilityEvent.findByProsumerId(client, prosumerId);
    }

    @POST
    public Uni<Response> create(FlexibilityEvent event) {
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        return event.save(client)
                .onItem().invoke(saved -> {
                    if (saved != null) {
                        offerEmitter.send(saved.toJson());
                    }
                })
                .onItem().transform(saved -> saved != null
                        ? Response.status(Response.Status.CREATED).entity(saved).build()
                        : Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteById(@PathParam("id") Long id) {
        return FlexibilityEvent.delete(client, id)
                .onItem().transform(deleted -> deleted
                        ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }
}
