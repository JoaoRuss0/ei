package org.acme;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TopicSubscription {

    private String topicName;
    private String ownerService;

    private static TopicSubscription from(Row row) {
        return new TopicSubscription(row.getString("topic_name"), row.getString("owner_service"));
    }

    public static Uni<TopicSubscription> findById(io.vertx.mutiny.mysqlclient.MySQLPool client, String topicName) {
        return client.preparedQuery("SELECT topic_name, owner_service FROM TopicSubscription WHERE topic_name = ?").execute(Tuple.of(topicName))
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
    }

    public Uni<Boolean> save(io.vertx.mutiny.mysqlclient.MySQLPool client)
    {
        return client.preparedQuery("INSERT INTO TopicSubscription(topic_name, owner_service) VALUES (?,?)").execute(Tuple.of(this.topicName, this.ownerService))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
    }

    public static Uni<Boolean> updateOwnerService(MySQLPool client, String topicName, String newOwnerService) {
        String query = """
                UPDATE TopicSubscription SET owner_service = ? WHERE topic_name = ?
                """;
        return client.preparedQuery(query).execute(Tuple.of(newOwnerService, topicName))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
    }

    public static Uni<Boolean> delete(io.vertx.mutiny.mysqlclient.MySQLPool client, String topicName) {
        return client.preparedQuery("DELETE FROM TopicSubscription WHERE topic_name = ?").execute(Tuple.of(topicName))
                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
    }
}
