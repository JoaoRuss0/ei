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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Asset {
	
	    public Long id;
		public String name;
		public Long prosumerId;

		private static Asset from(Row row) {
	        return new Asset(row.getLong("id"), row.getString("name"), row.getLong("prosumer_id"));
	    }
	    
	    public static Multi<Asset> findAll(MySQLPool client) {
	        return client.query("SELECT id, name, prosumer_id FROM Asset ORDER BY id ASC").execute()
	                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
	                .onItem().transform(Asset::from);
	    }
	    
	    public static Uni<Asset> findById(MySQLPool client, Long id) {
	        return client.preparedQuery("SELECT id, name, prosumer_id FROM Asset WHERE id = ?").execute(Tuple.of(id))
	                .onItem().transform(RowSet::iterator) 
	                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null); 
	    }

	    public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
	        return client.preparedQuery("DELETE FROM Asset WHERE id = ?").execute(Tuple.of(id_R))
	                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	    }

	    public Uni<Boolean> save(MySQLPool client)
		{
	        return client.preparedQuery("INSERT INTO Asset(name, prosumer_id) VALUES (?,?)").execute(Tuple.of( this.name, this.prosumerId))
	        		.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
	    }

	    public static Uni<Boolean> update(MySQLPool client, Long id, String name, Long prosumerId) {
	        return client.preparedQuery("UPDATE Asset SET name = ?, prosumer_id = ? WHERE id = ?")
					.execute(Tuple.of(name, prosumerId, id))
	        		.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
	    }  
}
