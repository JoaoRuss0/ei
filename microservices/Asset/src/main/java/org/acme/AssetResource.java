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
import java.util.Collection;
import java.util.List;

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
        .flatMap(r -> client.query("CREATE TABLE Asset (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, prosumer_id BIGINT UNSIGNED NOT NULL, grid_cell_id VARCHAR(255), asset_type ENUM('BATTERY','SOLAR','EV_CHARGER') NOT NULL)").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(name, prosumer_id, grid_cell_id, asset_type) VALUES ('asset-1', 1, 'PORTO_NORTH', 'BATTERY')").execute())
        .flatMap(r -> client.query("INSERT INTO Asset(name, prosumer_id, grid_cell_id, asset_type) VALUES ('asset-2', 1, 'PORTO_NORTH', 'BATTERY')").execute())
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
                .onItem().transform(id -> URI.create("/Asset/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }

    @POST
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
    @Path("active/by-grid-cell-ids")
    public Multi<Asset> getByCellIds(@QueryParam("cellIds") List<String> cellIds) {
        return Asset.findActiveByGridCellIds(client, cellIds);
    }

    @GET
    @Path("active/by-prosumer/{id}")
    public Multi<Asset> findActiveBatteriesByProsumerId(@PathParam("id") Long prosumerId) {
        return Asset.findActiveBatteriesByProsumerId(client, prosumerId);
    }
}
