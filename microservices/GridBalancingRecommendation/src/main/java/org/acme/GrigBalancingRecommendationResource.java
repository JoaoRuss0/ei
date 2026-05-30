package org.acme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("GridBalancing")
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
        client.query("DROP TABLE IF EXISTS GridBalancingRecommendation").execute()
        .flatMap(r -> client.query("CREATE TABLE GridBalancingRecommendation (grid_cell_from_id VARCHAR(255) NOT NULL, grid_cell_to_id VARCHAR(255) NOT NULL, transfer_kw DOUBLE NOT NULL, timestamp DATETIME NOT NULL, UNIQUE KEY UK_EVENT (grid_cell_from_id, grid_cell_to_id, timestamp))").execute())
        .flatMap(r -> client.query("INSERT INTO GridBalancingRecommendation (grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) VALUES ('PORTO_NORTH',    'PORTO_SOUTH',  22.0, '2026-04-15 19:00:00')").execute())
        .flatMap(r -> client.query("INSERT INTO GridBalancingRecommendation (grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) VALUES ('SETUBAL_CENTRO', 'LISBON_SOUTH',  5.0, '2026-04-18 19:30:00')").execute())
        .flatMap(r -> client.query("INSERT INTO GridBalancingRecommendation (grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) VALUES ('LISBON_NORTH',   'LISBON_SOUTH',  8.0, '2026-04-19 10:00:00')").execute())
        .await().indefinitely();
    }

    @GET
    public Multi<GridBalancingRecommendation> get() {
        return GridBalancingRecommendation.findAll(client);
    }

    @POST
    @Path("balance")
    @Blocking
    public List<GridBalancingRecommendation> balance(GridBalancingRequestDTO dto) {

        Map<String, Double> loadByCell = dto.events().stream()
                .collect(Collectors.groupingBy(
                        TelemetryEvent::grid_cell_id,
                        Collectors.summingDouble(this::contribution)));

        Map<Coords, GridCellData> byCoords = dto.cells().stream()
                .collect(Collectors.toMap(c -> new Coords(c.xCoords(), c.yCoords()), c -> c));

        List<GridBalancingRecommendation> recommendations = new ArrayList<>();

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
                GridBalancingRecommendation recommendation = new GridBalancingRecommendation(cell.id(), neighbour.id(), transfer);
                recommendation.save(client).await().indefinitely();
                recommendationEmitter.send(recommendation.toJson());
                recommendations.add(recommendation);

                loadByCell.merge(cell.id(), -transfer, Double::sum);
                loadByCell.merge(neighbour.id(), transfer, Double::sum);

                overload -= transfer;
                if (overload <= 0) break;
            }
        }

        return recommendations;
    }

    private double contribution(TelemetryEvent t) {
        return switch (t.asset_type()) {
            case "BATTERY" -> t.Status().equals("ONLINE") && t.Current_Output() != null ? -t.Current_Output() : 0;
            case "EV_CHARGER" -> t.Plug_Status().equals("CHARGING") && t.Charging_Rate() != null ? t.Charging_Rate() : 0;
            case "SOLAR" -> t.Current_Generation() == null ? 0 : -t.Current_Generation();
            default -> 0;
        };
    }
}
