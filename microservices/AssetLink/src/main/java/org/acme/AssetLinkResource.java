package org.acme;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

@Path("AssetLink")
public class AssetLinkResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate ;

    @Inject
    KafkaTopicService topicService;

    static final List<Map.Entry<Long, String>> SEED_TOPICS = List.of(
            Map.entry(1L, "ArcoCegoLisbon"),
            Map.entry(2L, "PracadeBocage"),
            Map.entry(3L, "PracadaBoavista"),
            Map.entry(4L, "PracaDomFranciscoGomes"),
            Map.entry(5L, "PracadaBoavista")
    );

    @Inject
    @ConfigProperty(name = "myapp.seed.create-topics", defaultValue = "true")
    boolean seedCreateTopics;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
            if (seedCreateTopics) {
                createSeedTopics();
            }
        }
    }

    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS AssetLink").execute()
        .flatMap(r -> client.query("CREATE TABLE AssetLink (id SERIAL PRIMARY KEY, idProsumer BIGINT UNSIGNED, idUtilityOperator BIGINT UNSIGNED, CONSTRAINT UC_Loyal UNIQUE (idProsumer,idUtilityOperator))").execute())
        .flatMap(r -> client.query("INSERT INTO AssetLink (id,idProsumer,idUtilityOperator) VALUES (1,1,1)").execute())
        .flatMap(r -> client.query("INSERT INTO AssetLink (id,idProsumer,idUtilityOperator) VALUES (2,2,2)").execute())
        .flatMap(r -> client.query("INSERT INTO AssetLink (id,idProsumer,idUtilityOperator) VALUES (3,3,3)").execute())
        .flatMap(r -> client.query("INSERT INTO AssetLink (id,idProsumer,idUtilityOperator) VALUES (4,4,4)").execute())
        .flatMap(r -> client.query("INSERT INTO AssetLink (id,idProsumer,idUtilityOperator) VALUES (5,1,3)").execute())
        .await().indefinitely();
    }

    private void createSeedTopics() {
        for (Map.Entry<Long, String> entry : SEED_TOPICS) {
            topicService.createAssetLinkTopic(entry.getKey(), entry.getValue());
        }
    }

    @GET
    public Multi<AssetLink> get() {
        return AssetLink.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return AssetLink.findById(client, id)
                .onItem().transform(assetlink -> assetlink != null ? Response.ok(assetlink) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build);
    }

    @GET
    @Path("by-prosumer-id/{idProsumer}")
    public Multi<AssetLink> getByProsumer(Long idProsumer) {
        return AssetLink.findByIdProsumerId(client, idProsumer);
    }

    @GET
    @Path("by-utilityoperator-id/{idUtilityOperator}")
    public Multi<AssetLink> getByUtilityOperator(Long idUtilityOperator) {
        return AssetLink.findByIdUtilityOperatorId(client, idUtilityOperator);
    }

    @POST
    public Uni<Response> create(AssetLink assetlink) {
        return assetlink.save(client , assetlink.idProsumer , assetlink.idUtilityOperator)
                .onItem().transform(id -> Response.created(URI.create("/AssetLink/" + id))
                        .entity(java.util.Map.of("id", id)).type(MediaType.APPLICATION_JSON)
                        .build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return AssetLink.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @POST
    @Path("topic/{assetLinkId}/{utilityOperatorName}")
    @Blocking
    public Response createTopic(Long assetLinkId, String utilityOperatorName) {
        topicService.createAssetLinkTopic(assetLinkId, utilityOperatorName);
        return Response.noContent().build();
    }

    @DELETE
    @Path("topic/{assetLinkId}/{utilityOperatorName}")
    @Blocking
    public Response deleteTopic(Long assetLinkId, String utilityOperatorName) {
        topicService.deleteAssetLinkTopic(assetLinkId, utilityOperatorName);
        return Response.noContent().build();
    }
}
