package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class EnergyAnalyticsResourceTest {

    @Inject
    MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM EnergyAnalytics").execute()
                .flatMap(r -> client.query("ALTER TABLE EnergyAnalytics AUTO_INCREMENT = 1").execute())
                .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (id, type, value, timestamp, grid_cell_id) " +
                        "VALUES (1, 'ENERGY_DISCHARGED_BY_ZONE', 34.2, '2026-01-01 20:00:00', 'PORTO_NORTH')").execute())
                .flatMap(r -> client.query("INSERT INTO EnergyAnalytics (id, type, value, timestamp, prosumer_id, prosumer_name) " +
                        "VALUES (2, 'ENERGY_GENERATED_BY_PROSUMER', 50.0, '2026-01-01 20:00:00', 2, 'client2')").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAllAnalytics() {
        given()
                .when().get("/EnergyAnalytics")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("type", hasItems("ENERGY_DISCHARGED_BY_ZONE", "ENERGY_GENERATED_BY_PROSUMER"));
    }

    @Test
    void testSaveAllAnalyticsBatch() {
        String payload = """
                [
                  {
                    "type": "AVERAGE_SOC",
                    "value": 75.5,
                    "timestamp": "2026-05-01T10:00:00",
                    "gridCellId": "PORTO_NORTH"
                  },
                  {
                    "type": "ENERGY_CONSUMED_BY_PROSUMER",
                    "value": 120.0,
                    "timestamp": "2026-05-01T10:00:00",
                    "prosumerId": 1,
                    "prosumerName": "client1"
                  }
                ]
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/EnergyAnalytics/save")
                .then()
                .statusCode(200)
                .body("saved", is(2));

        given()
                .when().get("/EnergyAnalytics")
                .then()
                .statusCode(200)
                .body("size()", is(4));
    }

    @Test
    void testSaveAllAnalyticsEmptyBatch() {
        given()
                .contentType(ContentType.JSON)
                .body("[]")
                .when().post("/EnergyAnalytics/save")
                .then()
                .statusCode(200)
                .body("saved", is(0));
    }

    @Test
    void testGetByIdSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/EnergyAnalytics/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("type", is("ENERGY_DISCHARGED_BY_ZONE"))
                .body("gridCellId", is("PORTO_NORTH"));
    }

    @Test
    void testGetByIdNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/EnergyAnalytics/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteByIdSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/EnergyAnalytics/{id}")
                .then()
                .statusCode(204);

        given().pathParam("id", 1).when().get("/EnergyAnalytics/{id}").then().statusCode(404);

        given().when().get("/EnergyAnalytics").then().statusCode(200).body("size()", is(1));
    }

    @Test
    void testDeleteByIdNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/EnergyAnalytics/{id}")
                .then()
                .statusCode(404);
    }
}
