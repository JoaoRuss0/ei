package org.acme;

import io.quarkus.arc.All;
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
public class UtilityOperator {

    public Long id;
    public String name;
    public String location;
    public String iban;

    private static UtilityOperator from(Row row) {
        return new UtilityOperator(row.getLong("id"), row.getString("name"), row.getString("location"), row.getString("iban"));
    }

    public static Multi<UtilityOperator> findAll(MySQLPool client) {
        return client.query("SELECT id, name, location, iban FROM UtilityOperator ORDER BY id ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(UtilityOperator::from);
    }

    public static Uni<UtilityOperator> findById(MySQLPool client, Long id) {
        return client.preparedQuery("SELECT id, name, location, iban FROM UtilityOperator WHERE id = ?").execute(Tuple.of(id))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public Uni<Long> save(MySQLPool client, String name_R, String loc, String iban) {
        return client.preparedQuery("INSERT INTO UtilityOperator(name,location, iban) VALUES (?,?,?)")
                .execute(Tuple.of(name_R, loc, iban))
                .onItem().transform(pgRowSet -> pgRowSet.property(MySQLClient.LAST_INSERTED_ID));
    }

    public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
        return client.preparedQuery("DELETE FROM UtilityOperator WHERE id = ?").execute(Tuple.of(id_R))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }

    public static Uni<Boolean> update(MySQLPool client, Long id_R, String name_R, String loc, String iban) {
        return client.preparedQuery("UPDATE UtilityOperator SET name = ?, location = ?, iban = ? WHERE id = ?")
                .execute(Tuple.of(name_R, loc, iban, id_R))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
}
