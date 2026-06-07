package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLClient;
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

    public Long id;
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
        return new GridBalancingRecommendation(row.getLong("id"), row.getString("grid_cell_from_id"), row.getString("grid_cell_to_id"), row.getDouble("transfer_kw"), row.getLocalDateTime("timestamp"));
    }

    public static Multi<GridBalancingRecommendation> findAll(MySQLPool client) {
        return client.query("SELECT * FROM GridBalancingRecommendation ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridBalancingRecommendation::from);
    }

    public static Uni<GridBalancingRecommendation> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM GridBalancingRecommendation WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Uni<Boolean> delete(MySQLPool client, Long id) {
        return client.preparedQuery("DELETE FROM GridBalancingRecommendation WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(rs -> rs.rowCount() == 1);
    }

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO GridBalancingRecommendation(grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) VALUES (?,?,?,?)").execute(Tuple.of(this.gridCellFromId, this.gridCellToId, this.transferKw, this.timestamp))
                .onItem().invoke(rs -> {
                    if (rs.rowCount() == 1) {
                        this.id = rs.property(MySQLClient.LAST_INSERTED_ID);
                    }
                })
                .onItem().transform(rs -> rs.rowCount() == 1);
    }

    public String toJson() {
        return String.format("{\"id\":%d,\"from\":\"%s\",\"to\":\"%s\",\"transfer_kw\":%f,\"timestamp\":%s}", this.id, this.gridCellFromId, this.gridCellToId, this.transferKw, this.timestamp.toString());
    }
}
