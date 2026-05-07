package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import java.time.LocalDateTime;

public class GridCell {
	
	    public Long id;
		public String location;
		public LocalDateTime peakHoursStartTime;
		public LocalDateTime peakHoursEndTime;
		public Long maxLoad;
		public Long operatorId;

	    public GridCell() {
	    }

		public GridCell(Long id, String location, LocalDateTime peakHoursStartTime, LocalDateTime peakHoursEndTime, Long maxLoad, Long operatorId) {
			this.id = id;
			this.location = location;
			this.peakHoursStartTime = peakHoursStartTime;
			this.peakHoursEndTime = peakHoursEndTime;
			this.maxLoad = maxLoad;
			this.operatorId = operatorId;
		}


		@Override
		public String toString() {
			return "{id:" + id + ", location:" + location + ", peakHoursStartTime:" + peakHoursStartTime.toString() + ", peakHoursEndTime:" + peakHoursEndTime.toString() + ", maxLoad:" + maxLoad.toString() + "}\n";
		}

		private static GridCell from(Row row) {
	        return new GridCell(row.getLong("id"), row.getString("location") , row.getLocalDateTime("peak_hours_start"), row.getLocalDateTime("peak_hours_end"), row.getLong("max_load"), row.getLong("operator_id"));
	    }
	    
	    public static Multi<GridCell> findAll(MySQLPool client) {
	        return client.query("SELECT id, location, peak_hours_start, peak_hours_end, max_load, operator_id FROM GridCell ORDER BY id ASC").execute()
	                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
	                .onItem().transform(GridCell::from);
	    }
	    
	    public static Uni<GridCell> findById(MySQLPool client, Long id) {
	        return client.preparedQuery("SELECT id, location, peak_hours_start, peak_hours_end, max_load, operator_id FROM GridCell WHERE id = ?").execute(Tuple.of(id))
	                .onItem().transform(RowSet::iterator) 
	                .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null); 
	    }

	    public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
	        return client.preparedQuery("DELETE FROM GridCell WHERE id = ?").execute(Tuple.of(id_R))
	                .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	    }

	    public Uni<Boolean> save(MySQLPool client , String location , LocalDateTime peakHoursStartTime, LocalDateTime peakHoursEndTime, Long maxLoad, Long operatorId)
		{
	        return client.preparedQuery("INSERT INTO GridCell(location,peak_hours_start,peak_hours_end, max_load, operator_id) VALUES (?,?,?,?,?)").execute(Tuple.of( location , peakHoursStartTime, peakHoursEndTime, maxLoad, operatorId))
	        		.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 );
	    }

	    public static Uni<Boolean> update(MySQLPool client, Long id, String location, LocalDateTime peakHoursStartTime, LocalDateTime peakHoursEndTime,  Long maxLoad, Long operatorId) {
	        return client.preparedQuery("UPDATE GridCell SET location = ? , peak_hours_start = ?, peak_hours_end = ?, max_load = ?, operatorId = ? WHERE id = ?").execute(Tuple.of( location, peakHoursStartTime, peakHoursEndTime, maxLoad, operatorId, id))
	        		.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1 ); 
	    }  
}
