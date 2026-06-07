package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GridBalancingRecommendationResourceTest {

    @Inject MySQLPool client;
    @Inject @Any InMemoryConnector connector;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM GridBalancingRecommendation").execute()
                .flatMap(r -> client.query("ALTER TABLE GridBalancingRecommendation AUTO_INCREMENT = 1").execute())
                .await().indefinitely();
        connector.sink("balancing-recommendation").clear();
    }

    @Test
    void testGetEndpoint() {
        client.query("INSERT INTO GridBalancingRecommendation (grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) " +
                        "VALUES ('TEST_FROM', 'TEST_TO', 50.0, NOW())")
                .execute().await().indefinitely();

        given()
                .when().get("/GridBalancing")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void testBalanceEndpointWithSuccessfulTransfer() {
        String jsonPayload = """
                {
                  "cells": [
                    {"id": "CELL_OVERLOADED", "xCoords": 0, "yCoords": 0, "maxLoad": 50.0},
                    {"id": "CELL_HEADROOM", "xCoords": 0, "yCoords": 1, "maxLoad": 100.0},
                    {"id": "CELL_ISOLATED", "xCoords": 5, "yCoords": 5, "maxLoad": 100.0}
                  ],
                  "events": [
                    {
                      "asset_id": 1,
                      "asset_type": "EV_CHARGER",
                      "grid_cell_id": "CELL_OVERLOADED",
                      "Plug_Status": "CHARGING",
                      "Charging_Rate": 80.0
                    },
                    {
                      "asset_id": 2,
                      "asset_type": "SOLAR",
                      "grid_cell_id": "CELL_HEADROOM",
                      "Current_Generation": 20.0
                    }
                  ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonPayload)
                .when().post("/GridBalancing/balance")
                .then()
                .statusCode(200);

        InMemorySink<String> sink = connector.sink("balancing-recommendation");

        assertEquals(1, sink.received().size(), "Should have emitted exactly one balancing recommendation");
        String emittedJson = sink.received().get(0).getPayload();

        org.junit.jupiter.api.Assertions.assertTrue(emittedJson.contains("CELL_OVERLOADED"));
        org.junit.jupiter.api.Assertions.assertTrue(emittedJson.contains("CELL_HEADROOM"));

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    RowSet<Row> rows = client.query("SELECT * FROM GridBalancingRecommendation").execute().await().indefinitely();

                    assertEquals(1, rows.size(), "Database should have saved 1 recommendation");

                    Row row = rows.iterator().next();
                    assertEquals("CELL_OVERLOADED", row.getString("grid_cell_from_id"));
                    assertEquals("CELL_HEADROOM", row.getString("grid_cell_to_id"));
                    assertEquals(30.0, row.getDouble("transfer_kw"), 0.01, "Transfer amount should perfectly match the overload amount");
                });
    }

    @Test
    void testGetByIdSuccess() {
        client.query("INSERT INTO GridBalancingRecommendation (id, grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) " +
                        "VALUES (1, 'A', 'B', 12.5, '2026-04-15 19:00:00')")
                .execute().await().indefinitely();

        given()
                .pathParam("id", 1)
                .when().get("/GridBalancing/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("gridCellFromId", is("A"))
                .body("gridCellToId", is("B"));
    }

    @Test
    void testGetByIdNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/GridBalancing/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteByIdSuccess() {
        client.query("INSERT INTO GridBalancingRecommendation (id, grid_cell_from_id, grid_cell_to_id, transfer_kw, timestamp) " +
                        "VALUES (1, 'A', 'B', 12.5, '2026-04-15 19:00:00')")
                .execute().await().indefinitely();

        given()
                .pathParam("id", 1)
                .when().delete("/GridBalancing/{id}")
                .then()
                .statusCode(204);

        given().pathParam("id", 1).when().get("/GridBalancing/{id}").then().statusCode(404);
    }

    @Test
    void testDeleteByIdNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/GridBalancing/{id}")
                .then()
                .statusCode(404);
    }
}