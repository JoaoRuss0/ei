package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

@Path("Asset")
public class AssetResource {

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
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS Asset").execute()
        .flatMap(r -> client.query("CREATE TABLE Asset (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, prosumer_id BIGINT UNSIGNED NOT NULL, asset_type ENUM('BATTERY','SOLAR','EV_CHARGER') NOT NULL)").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (1, 'lisbon-battery-1',   1, 'BATTERY')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (2, 'lisbon-solar-1',     1, 'SOLAR')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (3, 'setubal-battery-1',  2, 'BATTERY')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (4, 'setubal-ev-1',       2, 'EV_CHARGER')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (5, 'porto-solar-1',      3, 'SOLAR')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (6, 'porto-battery-1',    3, 'BATTERY')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (7, 'porto-ev-1',         3, 'EV_CHARGER')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (8, 'faro-solar-1',       4, 'SOLAR')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(id, name, prosumer_id, asset_type) VALUES (9, 'faro-ev-1',          4, 'EV_CHARGER')").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<Asset> get() {
        return Asset.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return Asset.findById(client, id)
                .onItem().transform(asset -> asset != null ? Response.ok(asset) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build); 
    }

    @POST
    public Uni<Response> create(Asset asset) {
        return asset.save(client)
                .onItem().transform(id -> Response.created(URI.create("/Asset/" + id))
                        .entity(java.util.Map.of("id", id))
                        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                        .build());
    }

    @PUT
    @Path("{id}")
    public Uni<Response> update(Long id, Asset asset) {
        return asset.update(client, id)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return Asset.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @GET
    @Path("by-prosumer/{id}")
    public Multi<Asset> findByProsumerId(@PathParam("id") Long prosumerId, @QueryParam("type") String type) {
        return Asset.findByProsumerId(client, prosumerId, type);
    }
}
