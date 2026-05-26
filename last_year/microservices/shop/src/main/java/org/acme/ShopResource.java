package org.acme;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

@Path("Shop")
public class ShopResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query(
                "CREATE TABLE IF NOT EXISTS Shops (id SERIAL PRIMARY KEY, name TEXT NOT NULL, address TEXT NOT NULL, postalCode TEXT NOT NULL)")
                .execute()
                .await().indefinitely();
    }

    @GET
    public Multi<Shop> get() {
        return Shop.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return Shop.findById(client, id)
                .onItem()
                .transform(shop -> shop != null ? Response.ok(shop) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build);
    }

    @POST
    public Uni<Response> create(Shop shop) {
        return shop.save(client, shop.name, shop.address, shop.postalCode)
                .onItem().transform(id -> {
                    if (id == null) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Failed to create Shop").build();
                    }
                    return Response.created(URI.create("/shop/" + id))
                            .entity(Map.of("id", id))
                            .build();
                });
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return Shop.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> update(Long id, Shop shop) {
        return Shop.update(client, id, shop.name, shop.address, shop.postalCode)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @GET
    @Path("health")
    public Uni<Response> health() {
        return Shop.checkDatabaseConnection(client)
                .onItem().transform(this::buildHealthResponse)
                .onFailure().recoverWithItem(this::buildErrorResponse);
    }

    private Response buildHealthResponse(Boolean dbHealthy) {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", dbHealthy ? "UP" : "DOWN");
        healthStatus.put("timestamp", System.currentTimeMillis());

        Map<String, Object> checks = new HashMap<>();
        checks.put("database", dbHealthy ? "UP" : "DOWN");

        healthStatus.put("checks", checks);

        Response.Status responseStatus = dbHealthy ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;
        return Response.status(responseStatus).entity(healthStatus).build();
    }

    private Response buildErrorResponse(Throwable throwable) {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "DOWN");
        healthStatus.put("timestamp", System.currentTimeMillis());
        healthStatus.put("error", throwable.getMessage());

        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(healthStatus)
                .build();
    }
}
