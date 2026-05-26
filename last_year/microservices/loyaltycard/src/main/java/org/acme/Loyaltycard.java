package org.acme;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Loyaltycard {

	public Long id;
	public Long idCustomer;
	public Long idShop;
	public Long[] _idsShops;

	public Loyaltycard() {
	}

	public Loyaltycard(Long id, Long idCustomer, Long[] idsShops) {
		this.id = id;
		this.idCustomer = idCustomer;
		this._idsShops = idsShops;
		this.idShop = null; // idShop is not used when _idsShops is present
	}

	public Loyaltycard(Long id, Long idCustomer, Long idShop) {
		this.id = id;
		this.idCustomer = idCustomer;
		this.idShop = idShop;
		this._idsShops = null; // _idsShops is not used when idShop is present
	}

	@Override
	public String toString() {
		String string = "{id:" + id + ", idCustomer:" + idCustomer;
		if (idShop != null) {
			string += ", idShop:" + idShop;
		}
		if (_idsShops != null && _idsShops.length > 0) {
			string += ", idsShops:" + java.util.Arrays.toString(_idsShops);
		}
		string += "}\n";
		return string;
	}

	private static Loyaltycard from(Row row) {
		try {
			if (row.getLong("idShop") != null) {
				return new Loyaltycard(row.getLong("id"), row.getLong("idCustomer"), row.getLong("idShop"));
			}
		} catch (Exception e) {
			// Handle the case where idShop column is not present
			// This means that the idsShops column is being used instead
		}

		Long[] idsShops = row.getString("idsShops") != null
				? java.util.Arrays.stream(row.getString("idsShops").split(","))
						.map(Long::valueOf)
						.toArray(Long[]::new)
				: new Long[0];

		return new Loyaltycard(row.getLong("id"), row.getLong("idCustomer"), idsShops);
	}

	public static Multi<Loyaltycard> findAll(MySQLPool client) {
		return client.query("SELECT id, idCustomer, idShop  FROM LoyaltyCards ORDER BY id ASC").execute()
				.onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
				.onItem().transform(Loyaltycard::from);
	}

	public static Uni<Loyaltycard> findById(MySQLPool client, Long id) {
		return client.preparedQuery(
				"SELECT id, idCustomer,  IFNULL(GROUP_CONCAT(idShop), '') AS idsShops FROM LoyaltyCards WHERE id = ?  GROUP BY id, idCustomer ORDER BY id ASC;")
				.execute(Tuple.of(id))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);
	}

	public static Uni<Loyaltycard> findByIdCustomerAndIdShop(MySQLPool client, Long idCustomer, Long idShop) {
		return client
				.preparedQuery("SELECT id, idCustomer, idShop FROM LoyaltyCards WHERE idCustomer = ? AND idShop = ?")
				.execute(Tuple.of(idCustomer, idShop))
				.onItem().transform(RowSet::iterator)
				.onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null);

	}

	public Uni<Long> save(MySQLPool client, Long idCustomer_R, Long idShop_R) {
		return client.preparedQuery("INSERT INTO LoyaltyCards(id,idCustomer,idShop) VALUES (?,?,?)")
				.execute(Tuple.of(idCustomer_R, idCustomer_R, idShop_R))
				.onItem().transform(pgRowSet -> {
					if (pgRowSet.rowCount() == 1) {
						return idCustomer_R;
					} else {
						return null;
					}
				});
	}

	public static Uni<Boolean> delete(MySQLPool client, Long id_R) {
		return client.preparedQuery("DELETE FROM LoyaltyCards WHERE id = ?").execute(Tuple.of(id_R))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() > 0);
	}

	public static Uni<Boolean> delete2(MySQLPool client, Long idCustomer_R, Long idShop_R) {
		return client.preparedQuery("DELETE FROM LoyaltyCards WHERE idCustomer = ? AND idShop = ?")
				.execute(Tuple.of(idCustomer_R, idShop_R))
				.onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1);
	}

	public static Uni<Boolean> checkDatabaseConnection(MySQLPool client) {
		return client.query("SELECT 1")
				.execute()
				.onItem().transform(result -> true)
				.onFailure().recoverWithItem(false);
	}
}
