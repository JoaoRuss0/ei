package org.acme;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.acme.model.Topic;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("Purchase")
public class KafkaProvisioningResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafka_servers;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        client.query("CREATE TABLE IF NOT EXISTS Purchases (" +
                "id SERIAL PRIMARY KEY," +
                "DateTime DATETIME," +
                "Price FLOAT," +
                "Product TEXT NOT NULL," +
                "Supplier TEXT NOT NULL," +
                "shopid BIGINT UNSIGNED," +
                "loyaltycardid BIGINT UNSIGNED," +
                "discountcouponid BIGINT UNSIGNED," +
                "CONSTRAINT unique_purchase_all_columns UNIQUE (" +
                "DateTime, Price, Product(200), Supplier(200), shopid, loyaltycardid, discountcouponid))")
                .execute()
                .await().indefinitely();
    }

    @POST
    @Path("Consume")
    public String ProvisioningConsumer(Topic topic) {
        Thread worker = new DynamicTopicConsumer(topic.TopicName, kafka_servers, client);
        worker.start();
        return "New worker started";
    }

    @GET
    public Multi<Purchase> get() {
        return Purchase.findAll(client);
    }

    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return Purchase.findById(client, id)
                .onItem()
                .transform(purchase -> purchase != null ? Response.ok(purchase)
                        : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build);
    }

    @GET
    @Path("health")
    public Uni<Response> health() {
        return Purchase.checkDatabaseConnection(client)
                .onItem().transform(this::buildHealthResponse)
                .onFailure().recoverWithItem(this::buildErrorResponse);
    }

    private Response buildHealthResponse(Boolean dbHealthy) {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", dbHealthy ? "UP" : "DOWN");
        healthStatus.put("timestamp", System.currentTimeMillis());

        Map<String, Object> checks = new HashMap<>();
        checks.put("database", dbHealthy ? "UP" : "DOWN");
        checks.put("kafka_servers", kafka_servers != null ? "CONFIGURED" : "NOT_CONFIGURED");

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
