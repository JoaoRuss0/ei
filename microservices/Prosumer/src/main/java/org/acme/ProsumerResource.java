package org.acme;

import java.net.URI;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.MediaType;

@Path("Prosumer")
public class ProsumerResource {

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
        client.query("DROP TABLE IF EXISTS Prosumer").execute()
        .flatMap(r -> client.query("CREATE TABLE Prosumer (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, FiscalNumber BIGINT UNSIGNED UNIQUE, location VARCHAR(255) NOT NULL)").execute())
        .flatMap(r -> client.query("INSERT INTO Prosumer (id,name,FiscalNumber,location) VALUES (1,'Maria Lisbon','123456','Lisbon')").execute())
        .flatMap(r -> client.query("INSERT INTO Prosumer (id,name,FiscalNumber,location) VALUES (2,'Joao Setubal','987654','Setubal')").execute())
        .flatMap(r -> client.query("INSERT INTO Prosumer (id,name,FiscalNumber,location) VALUES (3,'Pedro Porto','123987','Porto')").execute())
        .flatMap(r -> client.query("INSERT INTO Prosumer (id,name,FiscalNumber,location) VALUES (4,'Ana Faro','987123','Faro')").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<Prosumer> get() {
        return Prosumer.findAll(client);
    }
    
    @GET
    @Path("{id}")
    public Uni<Response> getSingle(Long id) {
        return Prosumer.findById(client, id)
                .onItem().transform(prosumer -> prosumer != null ? Response.ok(prosumer) : Response.status(Response.Status.NOT_FOUND)) 
                .onItem().transform(ResponseBuilder::build); 
    }
     
    @POST
    public Uni<Response> create(Prosumer prosumer) {
        return prosumer.save(client , prosumer.name , prosumer.FiscalNumber , prosumer.location)
                .onItem().transform(id -> Response.created(URI.create("/Prosumer/" + id))
                        .entity(java.util.Map.of("id", id))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }
    
    @DELETE
    @Path("{id}")
    public Uni<Response> delete(Long id) {
        return Prosumer.delete(client, id)
                .onItem().transform(deleted -> deleted ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> update(Long id, ProsumerUpdateRequest request) {
        return Prosumer.update(client, id, request.name(), request.FiscalNumber(), request.location())
                .onItem().transform(updated -> updated ? Response.Status.NO_CONTENT : Response.Status.NOT_FOUND)
                .onItem().transform(status -> Response.status(status).build());
    }
    
}
