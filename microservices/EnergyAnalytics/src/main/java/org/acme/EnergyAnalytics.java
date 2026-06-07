package org.acme;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EnergyAnalytics {

    public Long id;
    public Long prosumerId;
    public String prosumerName;
    public Long utilityOperatorId;
    public String utilityOperatorName;
    public EnergyAnalyticsType type;
    public String gridCellId;
    public Double value;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime timestamp;

    private static EnergyAnalytics from(Row row) {
        return new EnergyAnalytics(
                row.getLong("id"),
                row.getLong("prosumer_id"),
                row.getString("prosumer_name"),
                row.getLong("utility_operator_id"),
                row.getString("utility_operator_name"),
                EnergyAnalyticsType.valueOf(row.getString("type")),
                row.getString("grid_cell_id"),
                row.getDouble("value"),
                row.getLocalDateTime("timestamp"));
    }

    public static Multi<EnergyAnalytics> findAll(MySQLPool client) {
        return client.query("SELECT * FROM EnergyAnalytics ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(EnergyAnalytics::from);
    }

    public static Uni<EnergyAnalytics> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM EnergyAnalytics WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Uni<Boolean> delete(MySQLPool client, Long id) {
        return client.preparedQuery("DELETE FROM EnergyAnalytics WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(rs -> rs.rowCount() == 1);
    }

    public Uni<Boolean> save(MySQLPool client) {
        Tuple params = Tuple.tuple()
                .addLong(this.prosumerId)
                .addString(this.prosumerName)
                .addLong(this.utilityOperatorId)
                .addString(this.utilityOperatorName)
                .addString(this.type.name())
                .addString(this.gridCellId)
                .addDouble(this.value)
                .addLocalDateTime(this.timestamp);

        return client.preparedQuery(
                        "INSERT INTO EnergyAnalytics(prosumer_id, prosumer_name, utility_operator_id, utility_operator_name, type, grid_cell_id, value, timestamp) "
                                + "VALUES (?,?,?,?,?,?,?,?)")
                .execute(params)
                .onItem().transform(rowSet -> rowSet.rowCount() == 1);
    }


    public String toJson() {
        return new JsonObject()
                .put("id", this.id)
                .put("type", this.type == null ? null : this.type.name())
                .put("prosumerId", this.prosumerId)
                .put("prosumerName", this.prosumerName)
                .put("utilityOperatorId", this.utilityOperatorId)
                .put("utilityOperatorName", this.utilityOperatorName)
                .put("gridCellId", this.gridCellId)
                .put("value", this.value)
                .put("timestamp", this.timestamp == null ? null : this.timestamp.toString())
                .encode();
    }

    public static Uni<Integer> saveAll(MySQLPool client, List<EnergyAnalytics> analytics) {
        if (analytics == null || analytics.isEmpty()) {
            return Uni.createFrom().item(0);
        }
        List<Tuple> batch = analytics.stream()
                .map(a -> Tuple.tuple()
                        .addLong(a.prosumerId)
                        .addString(a.prosumerName)
                        .addLong(a.utilityOperatorId)
                        .addString(a.utilityOperatorName)
                        .addString(a.type.name())
                        .addString(a.gridCellId)
                        .addDouble(a.value)
                        .addLocalDateTime(a.timestamp))
                .collect(Collectors.toList());

        return client.preparedQuery(
                        "INSERT INTO EnergyAnalytics(prosumer_id, prosumer_name, utility_operator_id, utility_operator_name, type, grid_cell_id, value, timestamp) "
                                + "VALUES (?,?,?,?,?,?,?,?)")
                .executeBatch(batch)
                .onItem().transform(EnergyAnalytics::countRows);
    }

    private static int countRows(RowSet<Row> rs) {
        int total = 0;
        for (RowSet<Row> r = rs; r != null; r = r.next()) {
            total += r.rowCount();
        }
        return total;
    }
}