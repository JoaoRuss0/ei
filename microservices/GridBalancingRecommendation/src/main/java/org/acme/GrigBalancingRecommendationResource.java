package org.acme;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.annotations.Channel;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("AssetLink")
public class GrigBalancingRecommendationResource {

    private static final long[][] NEIGHBOURS = {{0,1},{0,-1},{1,0},{-1,0}};

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;
    
    @Inject
    @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") 
    boolean schemaCreate ;

    @Channel("balancing-recommendation")
    Emitter<String> recommendationEmitter;

    void config(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }
    
    private void initdb() {
        // In a production environment this configuration SHOULD NOT be used
        client.query("DROP TABLE IF EXISTS AssetLink").execute()
        .flatMap(r -> client.query("CREATE TABLE GridBalancingRecommendation (id SERIAL PRIMARY KEY, event_time DATETIME NOT NULL)").execute())
        .flatMap(r -> client.query(" INSERT INTO GridBalancingRecommendation (event_time) VALUES ('2020-04-12T02:11')").execute())
        .flatMap(r -> client.query(" INSERT INTO GridBalancingRecommendation (event_time) VALUES ('2012-10-10T21:21')").execute())
        .flatMap(r -> client.query(" INSERT INTO GridBalancingRecommendation (event_time) VALUES ('2015-11-11T01:41')").execute())
        .flatMap(r -> client.query(" INSERT INTO GridBalancingRecommendation (event_time) VALUES ('2030-22-10T20:51')").execute())
        .await().indefinitely();
    }
    
    @GET
    public Multi<GridBalancingRecommendation> get() {
        return GridBalancingRecommendation.findAll(client);
    }

    @POST
    @Path("analyse")
    @Blocking
    public void analyse(AnalyseRequestDTO dto) {

        Map<String, Double> loadByCell = dto.events().stream()
                .collect(Collectors.groupingBy(
                        TelemetryEvent::grid_cell_id,
                        Collectors.summingDouble(this::contribution)));

        Map<Coords, GridCellData> byCoords = dto.cells().stream()
                .collect(Collectors.toMap(c -> new Coords(c.xCoords(), c.yCoords()), c -> c));

        for (GridCellData cell : dto.cells()) {
            double load = loadByCell.getOrDefault(cell.id(), 0.0);
            if (load <= cell.maxLoad()) continue;

            double overload = load - cell.maxLoad();

            for (long[] d : NEIGHBOURS) {
                Coords key = new Coords(cell.xCoords() + d[0], cell.yCoords() + d[1]);
                GridCellData neighbour = byCoords.get(key);
                if (neighbour == null) continue;

                double nLoad = loadByCell.getOrDefault(neighbour.id(), 0.0);
                double headroom = neighbour.maxLoad() - nLoad;
                if (headroom <= 0) continue;

                double transfer = Math.min(overload, headroom);
                recommendationEmitter.send(toJson(new Recommendation(cell.id(), neighbour.id(), transfer)));

                loadByCell.merge(cell.id(), -transfer, Double::sum);
                loadByCell.merge(neighbour.id(), transfer, Double::sum);

                overload -= transfer;
                if (overload <= 0) break;
            }
        }
    }

    private double contribution(TelemetryEvent t) {
        return switch (t.asset_type()) {
            case "BATTERY" -> t.Status().equals("ONLINE") && t.Current_Output() != null ? -t.Current_Output() : 0;
            case "EV_CHARGER" -> t.Plug_Status().equals("CHARGING") && t.Charging_Rate() != null ? t.Charging_Rate() : 0;
            case "SOLAR" -> t.Current_Generation() == null ? 0 : -t.Current_Generation();
            default -> 0;
        };
    }

    private static String toJson(Recommendation r) {
        return String.format("{\"from\":\"%s\",\"to\":\"%s\",\"amount_kw\":%.2f}",
                r.from(), r.to(), r.amountKw());
    }

}
