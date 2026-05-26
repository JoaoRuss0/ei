package org.acme;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@Path("CrossSellingRecommendation")
public class CrossSellingRecommendationResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafka_servers;

    @Channel("crossSellingRecommendation")
    Emitter<String> emitter;

    void config(@Observes StartupEvent ev) {

    }

    @POST
    public Uni<Response> create(CrossSellingRecommendation crossSellingRecommendation) {
        String message = crossSellingRecommendation.toString();
        emitter.send(message)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        System.err.println("Failed to send message to Kafka Topic: " + failure.getMessage());
                    }
                });
        return Uni.createFrom().item(Response.accepted().entity("Message sent to Kafka Topic: " + message).build());
    }

    @GET
    @Path("health")
    public Response health() {
        boolean kafkaConfigured = kafka_servers != null && !kafka_servers.trim().isEmpty();

        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", kafkaConfigured ? "UP" : "DOWN");
        healthStatus.put("timestamp", System.currentTimeMillis());

        Map<String, Object> checks = new HashMap<>();
        checks.put("kafka_servers", kafkaConfigured ? "CONFIGURED" : "NOT_CONFIGURED");

        healthStatus.put("checks", checks);

        Response.Status responseStatus = kafkaConfigured ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE;
        return Response.status(responseStatus).entity(healthStatus).build();
    }
}
