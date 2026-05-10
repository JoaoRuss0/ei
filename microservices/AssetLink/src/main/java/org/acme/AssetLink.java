package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLClient;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AssetLink {

    public Long id;
    public Long idProsumer;
    public Long idUtilityOperator;

    private static AssetLink from(Row row) {
        return new AssetLink(row.getLong("id"), row.getLong("idProsumer"), row.getLong("idUtilityOperator"));
    }

    public static Multi<AssetLink> findAll(MySQLPool client) {
        return client.query("SELECT id, idProsumer, idUtilityOperator  FROM AssetLink ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(AssetLink::from);
    }

    public static Uni<AssetLink> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT id, idProsumer, idUtilityOperator  FROM AssetLink WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public static Uni<AssetLink> findById2(MySQLPool client, Long idProsumer_R, Long idUtilityOperator_R) {
        return client.preparedQuery("SELECT id, idProsumer, idUtilityOperator FROM AssetLink WHERE idProsumer = ? AND idUtilityOperator = ?").execute(Tuple.of(idProsumer_R, idUtilityOperator_R))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);

    }

    public Uni<Long> save(MySQLPool client, Long idProsumer_R, Long idUtilityOperator_R) {
        return client.preparedQuery("INSERT INTO AssetLink(idProsumer, idUtilityOperator) VALUES (?,?)")
                .execute(Tuple.of(idProsumer_R, idUtilityOperator_R))
                .onItem().transform(pgRowSet -> pgRowSet.property(MySQLClient.LAST_INSERTED_ID));
    }

    public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
        return client.preparedQuery("DELETE FROM AssetLink WHERE id = ?").execute(Tuple.of(id_R))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
}
