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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Path("GridCell")
public class GridCellResource {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true")
    boolean schemaCreate ;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS GridCell").execute()
        .flatMap(r -> client.query("CREATE TABLE GridCell (id VARCHAR(255) PRIMARY KEY, address VARCHAR(255) NOT NULL, postal_code VARCHAR(255) NOT NULL, peak_hours_start DATETIME NOT NULL , peak_hours_end DATETIME NOT NULL , max_load BIGINT NOT NULL , operator_id BIGINT UNSIGNED NOT NULL, x_coords BIGINT NOT NULL, y_coords BIGINT NOT NULL, UNIQUE KEY UK_COORDS (x_coords, y_coords) )").execute())
        .flatMap(r -> client.query("INSERT INTO GridCell (id, address, postal_code, peak_hours_start, peak_hours_end, max_load, x_coords, y_coords, operator_id) VALUES ('PORTO_NORTH', 'Rua do Figo', '2222-232', '2026-01-01 18:00:00', '2026-01-01 21:00:00', 10, 1, 1, 1)").execute())
        .flatMap(r -> client.query("INSERT INTO GridCell (id, address, postal_code, peak_hours_start, peak_hours_end, max_load, x_coords, y_coords, operator_id) VALUES ('LISBON_SOUTH', 'Rua da Pera', '3142-521', '2026-01-01 09:30:00', '2026-01-01 12:00:00', 50, 1, 0, 2)").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<GridCell> get() {
        return GridCell.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(String id) {
        return GridCell.findById(client, id)
                .onItem().transform(gridCell -> gridCell != null ? Response.ok(gridCell) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build); 
    }
     
    @POST
    public Uni<Response> create(GridCell gridCell) {
        return gridCell.save(client)
                .onItem().transform(id -> Response.created(URI.create("/GridCell/" + id))
                        .entity(java.util.Map.of("id", id))
                        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                        .build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(String id) {
        return GridCell.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> update(String id , GridUpdateRequest request) {
        return GridCell.update(client, id, request)
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @GET
    @Path("by-ids")
    public Multi<GridCell> getByCellIds(@QueryParam("ids") List<String> ids) {
        return GridCell.findByIds(client, ids);
    }

    @GET
    @Path("by-operator-ids")
    public Multi<GridCell> getByOperatorIds(@QueryParam("operatorIds") List<Long> operatorIds) {
        return GridCell.findByOperatorIds(client, operatorIds);
    }
}
