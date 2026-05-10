package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class FlexibilityEvent {

    private static final AtomicLong nextId = new AtomicLong(0);

    public Long id;
    public Long assetId;
    public Long prosumerId;
    public LocalDateTime eventTime;
    public FlexibilityEventType eventType;

    public FlexibilityEvent(Long assetId, Long prosumerId, FlexibilityEventType eventType) {
        this.id = nextId.getAndIncrement();
        this.assetId = assetId;
        this.eventTime = LocalDateTime.now();
        this.prosumerId = prosumerId;
        this.eventType = eventType;
    }

    public FlexibilityEvent(Long id, Long assetId, Long prosumerId, FlexibilityEventType eventType, LocalDateTime eventTime) {
        this.id = id;
        this.assetId = assetId;
        this.eventTime = eventTime;
        this.prosumerId = prosumerId;
        this.eventType = eventType;
    }

    public String toJson() {
        return String.format(
                "{\"id\":%d,\"assetId\":%d,\"prosumerId\":%d,\"eventTime\":\"%s\",\"eventType\":\"%s\"}",
                id, assetId, prosumerId, eventTime.toString(), eventType.name());
    }

    private static FlexibilityEvent from(Row row) {
        return new FlexibilityEvent(row.getLong("id"), row.getLong("asset_id"), row.getLong("prosumer_id"), FlexibilityEventType.valueOf(row.getString("event_type")), row.getLocalDateTime("event_time"));
    }

    public static Multi<FlexibilityEvent> findAll(MySQLPool client) {
        return client.query("SELECT id, asset_id, prosumer_id, event_type, event_time FROM FlexibilityEvent ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(FlexibilityEvent::from);
    }

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO FlexibilityEvent(asset_id, prosumer_id, event_type, event_time) VALUES (?,?,?,?)").execute(Tuple.of(this.assetId, this.prosumerId, this.eventType.name(), this.eventTime.toString()))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
}
