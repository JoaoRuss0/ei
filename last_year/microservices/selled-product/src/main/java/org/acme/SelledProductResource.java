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

@Path("SelledProduct")
public class SelledProductResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafka_servers;

    @Channel("selledProductByLoyaltyCard")
    Emitter<String> emitterLoyaltyCard;

    @Channel("selledProductByShop")
    Emitter<String> emitterShop;

    @Channel("selledProductByLocation")
    Emitter<String> emitterLocation;

    @Channel("selledProductByCoupon")
    Emitter<String> emitterCoupon;

    void config(@Observes StartupEvent ev) {

    }

    private Emitter<String> getEmitterForType(SelledProduct.TypeOfAnalysis typeOfAnalysis) {
        switch (typeOfAnalysis) {
            case LOYALTY_CARD:
                return emitterLoyaltyCard;
            case DISCOUNT_COUPON:
                return emitterCoupon;
            case SHOP:
                return emitterShop;
            case POSTAL_CODE:
                return emitterLocation;
            default:
                throw new IllegalArgumentException("Unknown TypeOfAnalysis: " + typeOfAnalysis);
        }
    }

    @POST
    public Uni<Response> create(SelledProduct selledProduct) {
        String message = selledProduct.toString();
        Emitter<String> emitter = getEmitterForType(selledProduct.typeOfAnalysis);
        emitter.send(message)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        System.err.println("Failed to send message to Kafka Topic: "
                                + failure.getMessage());
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
