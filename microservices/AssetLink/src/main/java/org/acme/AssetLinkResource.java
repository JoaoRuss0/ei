package org.acme;

import java.net.URI;

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

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }
    
    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS AssetLink").execute()
        .flatMap(r -> client.query("CREATE TABLE AssetLink (id SERIAL PRIMARY KEY, idProsumer BIGINT UNSIGNED, idUtilityOperator BIGINT UNSIGNED, CONSTRAINT UC_Loyal UNIQUE (idProsumer,idUtilityOperator))").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (1,1)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (2,1)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (1,3)").execute())
        .flatMap(r -> client.query(" INSERT INTO AssetLink (idProsumer,idUtilityOperator) VALUES (4,2)").execute())
        .await().indefinitely();
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
    @Path("{idProsumer}/{idUtilityOperator}")
    public Uni<Response> getDual(Long idProsumer, Long idUtilityOperator) {
        return AssetLink.findById2(client, idProsumer, idUtilityOperator)
                .onItem().transform(assetlink -> assetlink != null ? Response.ok(assetlink) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }

    @GET
    @Path("by-prosumer-id/{idProsumer}")
    public Multi<AssetLink> getDual(Long idProsumer) {
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
    @Path("topic/{prosumerId}/{utilityOperatorId}")
    @Blocking
    public Response createTopic(Long prosumerId, Long utilityOperatorId) {
        topicService.createAssetLinkTopic(prosumerId, utilityOperatorId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("topic/{prosumerId}/{utilityOperatorId}")
    @Blocking
    public Response deleteTopic(Long prosumerId, Long utilityOperatorId) {
        topicService.deleteAssetLinkTopic(prosumerId, utilityOperatorId);
        return Response.noContent().build();
    }
}
