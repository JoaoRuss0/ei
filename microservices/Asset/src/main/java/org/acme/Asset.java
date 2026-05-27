package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLClient;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Asset {

    public Long id;
    public String name;
    public Long prosumerId;
    public AssetType type;

    private static Asset from(Row row) {
        return new Asset(row.getLong("id"), row.getString("name"), row.getLong("prosumer_id"), AssetType.valueOf(row.getString("asset_type")));
    }

    public static Multi<Asset> findAll(MySQLPool client) {
        return client.query("SELECT * FROM Asset ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(Asset::from);
    }

    public static Uni<Asset> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT * FROM Asset WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
        return client.preparedQuery("DELETE FROM Asset WHERE id = ?").execute(Tuple.of(id_R))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public Uni<Long> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO Asset(name, prosumer_id, asset_type) VALUES (?,?,?)")
                .execute(Tuple.of(this.name, this.prosumerId, this.type.name()))
                .onItem().transform(pgRowSet -> pgRowSet.property(MySQLClient.LAST_INSERTED_ID));
    }

    public Uni<Boolean> update(MySQLPool client, Long id) {
        return client.preparedQuery("UPDATE Asset SET name = ?, prosumer_id = ?, asset_type = ? WHERE id = ?").execute(Tuple.of(this.name, this.prosumerId, this.type.name(), id))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public static Multi<Asset> findByProsumerId(MySQLPool client, Long prosumerId, String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank()) {
            return client.preparedQuery("SELECT * FROM Asset WHERE prosumer_id = ?").execute(Tuple.of(prosumerId))
                    .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                    .onItem().transform(Asset::from);
        }
        return client.preparedQuery("SELECT * FROM Asset WHERE prosumer_id = ? AND asset_type = ?").execute(Tuple.of(prosumerId, typeFilter))
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(Asset::from);
    }
}
