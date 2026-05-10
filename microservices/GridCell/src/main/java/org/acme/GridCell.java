package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class GridCell {

    public String id;
    public String address;
    public String postalCode;
    public LocalDateTime peakHoursStartTime;
    public LocalDateTime peakHoursEndTime;
    public Long maxLoad;
    public Long operatorId;
    public Long xCoords;
    public Long yCoords;

    private static GridCell from(Row row) {
        return new GridCell(row.getString("id"), row.getString("address"),
                row.getString("postal_code"), row.getLocalDateTime("peak_hours_start"),
                row.getLocalDateTime("peak_hours_end"), row.getLong("max_load"),
                row.getLong("operator_id"), row.getLong("x_coords"), row.getLong("y_coords"));
    }

    public static Multi<GridCell> findAll(MySQLPool client) {
        return client.query("SELECT * FROM GridCell ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridCell::from);
    }

    public static Uni<GridCell> findById(MySQLPool client, String id) {
        return client.preparedQuery("SELECT * FROM GridCell WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Uni<Boolean> delete(MySQLPool client, String id_R) {
        return client.preparedQuery("DELETE FROM GridCell WHERE id = ?").execute(Tuple.of(id_R))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public Uni<Boolean> save(MySQLPool client) {
        Tuple params = Tuple.tuple(List.of(address, postalCode, peakHoursStartTime, peakHoursEndTime, maxLoad, operatorId, xCoords, yCoords));
        return client.preparedQuery("INSERT INTO GridCell(address, postal_code, peak_hours_start, peak_hours_end, max_load, operator_id, x_coords, y_coords) VALUES (?,?,?,?,?,?,?,?)").execute(params)
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public static Uni<Boolean> update(MySQLPool client, String id, GridUpdateRequest request) {
        Tuple params = Tuple.tuple(List.of(request.address(), request.postalCode(), request.peakHoursStartTime(), request.peakHoursEndTime(), request.maxLoad(), request.operatorId(), request.xCoords(), request.yCoords(), id));
        return client.preparedQuery("UPDATE GridCell SET address = ?, postal_code = ? , peak_hours_start = ?, peak_hours_end = ?, max_load = ?, operator_id = ?, x_coords = ?, y_coords = ? WHERE id = ?")
                .execute(params)
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public static Multi<GridCell> findByIds(MySQLPool client, List<Long> ids) {
        if (ids.isEmpty()) return Multi.createFrom().empty();

        String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
        String query = "SELECT * FROM GridCell WHERE id IN (" + placeholders + ")";

        Tuple params = Tuple.tuple();
        ids.forEach(params::addLong);

        return client.preparedQuery(query).execute(params)
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridCell::from);
    }

    public static Multi<GridCell> findByOperatorIds(MySQLPool client, List<Long> operatorIds) {
        if (operatorIds.isEmpty()) return Multi.createFrom().empty();

        String placeholders = operatorIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String query = "SELECT * FROM GridCell WHERE operator_id IN (" + placeholders + ")";

        Tuple params = Tuple.tuple();
        operatorIds.forEach(params::addLong);

        return client.preparedQuery(query).execute(params)
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(GridCell::from);
    }
}
