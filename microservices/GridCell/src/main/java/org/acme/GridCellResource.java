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
        .flatMap(r -> client.query("CREATE TABLE GridCell (id SERIAL PRIMARY KEY, Location TEXT NOT NULL, peak_hours_start DATETIME, peak_hours_end DATETIME, max_load BIGINT)").execute())
        .flatMap(r -> client.query("INSERT INTO GridCell (Location, peak_hours_start, peak_hours_end, max_load) VALUES ('Lisbon', '2026-01-01 18:00:00', '2026-01-01 21:00:00', 10)").execute())
        .flatMap(r -> client.query("INSERT INTO GridCell (Location, peak_hours_start, peak_hours_end, max_load) VALUES ('Porto', '2026-01-01 09:30:00', '2026-01-01 12:00:00', 50)").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<GridCell> get() {
        return GridCell.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return GridCell.findById(client, id)
                .onItem().transform(gridCell -> gridCell != null ? Response.ok(gridCell) : Response.status(Response.Status.NOT_FOUND))
                .onItem().transform(ResponseBuilder::build); 
    }
     
    @POST
    public Uni<Response> create(GridCell gridCell) {
        return gridCell.save(client , gridCell.location , gridCell.peakHoursStartTime, gridCell.peakHoursEndTime, gridCell.maxLoad)
                .onItem().transform(id -> URI.create("/GridCell/" + id))
                .onItem().transform(uri -> Response.created(uri).build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return GridCell.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> update(Long id , GridUpdateRequest request) {
        return GridCell.update(client, id, request.location(), request.peakHoursStart(), request.peakHoursEnd(), request.maxLoad())
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }
    
}
