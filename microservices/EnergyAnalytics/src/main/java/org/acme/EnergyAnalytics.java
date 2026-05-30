package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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

    public EnergyAnalyticsType type;
    public String entityId;
    public Double value;
    public LocalDateTime timestamp;

    private static EnergyAnalytics from(Row row) {
        return new EnergyAnalytics(EnergyAnalyticsType.valueOf(row.getString("type")), row.getString("entity_id"), row.getDouble("value"), row.getLocalDateTime("timestamp"));
    }

    public static Multi<EnergyAnalytics> findAll(MySQLPool client) {
        return client.query("SELECT *  FROM EnergyAnalytics").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(EnergyAnalytics::from);
    }

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO EnergyAnalytics(type, entity_id, value, timestamp) VALUES (?,?,?,?)").execute(Tuple.of(this.type.name(), this.entityId, this.value, this.timestamp))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public String toJson() {
        return String.format("{\"type\":\"%s\",\"entityId\":\"%s\",\"value\":%f,\"timestamp\":%s}", this.type.name(), this.entityId, this.value, this.timestamp.toString());
    }

    public static Uni<Integer> saveAll(MySQLPool client, List<EnergyAnalytics> analytics) {
        if (analytics == null || analytics.isEmpty()) {
            return Uni.createFrom().item(0);
        }
        List<Tuple> batch = analytics.stream()
                .map(a -> Tuple.of(a.type.name(), a.entityId, a.value, a.timestamp))
                .collect(Collectors.toList());
        return client.preparedQuery("INSERT INTO EnergyAnalytics(type, entity_id, value, timestamp) VALUES (?,?,?,?)")
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
