package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

import java.util.Collection;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Asset {

    public Long id;
    public String name;
    public Long prosumerId;
    public String gridCellId;
    public AssetType type;

    private static Asset from(Row row) {
        return new Asset(row.getLong("id"), row.getString("name"), row.getLong("prosumer_id"), row.getString("grid_cell_id"), AssetType.valueOf(row.getString("asset_type")));
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

    public Uni<Boolean> save(MySQLPool client) {
        return client.preparedQuery("INSERT INTO Asset(name, prosumer_id, grid_cell_id) VALUES (?,?,?)").execute(Tuple.of(this.name, this.prosumerId, this.gridCellId))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public Uni<Boolean> update(MySQLPool client, Long id) {
        return client.preparedQuery("UPDATE Asset SET name = ?, prosumer_id = ?, grid_cell_id = ? WHERE id = ?").execute(Tuple.of(this.name, this.prosumerId, this.gridCellId, id))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public static Multi<Asset> findActiveBatteriesByProsumerId(MySQLPool client, Long prosumerId) {
        return client.preparedQuery("SELECT * FROM Asset WHERE prosumer_id = ? and asset_type = 'BATTERY' and grid_cell_id IS NOT NULL").execute(Tuple.of(prosumerId))
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(Asset::from);
    }

    public static Multi<Asset> findActiveByGridCellIds(MySQLPool client, Collection<String> cellIds) {
        if (cellIds == null || cellIds.isEmpty()) return Multi.createFrom().empty();

        String placeholders = cellIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String query = "SELECT * FROM Asset WHERE grid_cell_id IN (" + placeholders + ")";

        Tuple params = Tuple.tuple();
        cellIds.forEach(params::addString);

        return client.preparedQuery(query).execute(params)
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(Asset::from);
    }
}
