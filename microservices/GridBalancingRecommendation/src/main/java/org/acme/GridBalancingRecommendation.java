package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GridBalancingRecommendation {

    public String gridCellFromId;
    public String gridCellToId;
    public Double transferKw;
    public LocalDateTime timestamp;

    public GridBalancingRecommendation(String gridCellFromId, String gridCellToId, Double transferKw) {
        this.gridCellFromId = gridCellFromId;
        this.gridCellToId = gridCellToId;
        this.transferKw = transferKw;
        this.timestamp = LocalDateTime.now();
    }

    private static GridBalancingRecommendation from(Row row) {
        return new GridBalancingRecommendation(row.getString("grid_cell_from_id"), row.getString("grid_cell_to_id"), row.getDouble("transfer_kw"), row.getLocalDateTime("timestamp"));
    }

    public static Multi<GridBalancingRecommendation> findAll(MySQLPool client) {
        return client.query("SELECT *  FROM GridBalancingRecommendation").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridBalancingRecommendation::from);
    }

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO GridBalancingRecommendation(grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) VALUES (?,?,?)").execute(Tuple.of(this.gridCellFromId, this.gridCellToId, this.transferKw, this.timestamp))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public String toJson() {
        return String.format("{\"from\":\"%s\",\"to\":\"%s\",\"transfer_kw\":%.2f}", this.gridCellFromId, this.gridCellToId, this.transferKw);
    }
}
