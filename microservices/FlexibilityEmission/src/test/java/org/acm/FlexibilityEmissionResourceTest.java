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
class FlexibilityEmissionResourceTest {

    @Inject
    MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM FlexibilityEvent").execute()
                .flatMap(r -> client.query("ALTER TABLE FlexibilityEvent AUTO_INCREMENT = 1").execute())
                .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (1, 1, 1, 'UNAVAILABLE_FOR_BALANCING', '2026-01-01 20:00')").execute())
                .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (2, 1, 1, 'SELL', '2026-01-01 21:00')").execute())
                .flatMap(r -> client.query("INSERT INTO FlexibilityEvent (id, asset_id, prosumer_id, event_type, event_time) VALUES (3, 2, 2, 'UNAVAILABLE_FOR_BALANCING', '2026-01-02 10:00')").execute())
                .await().indefinitely();
    }

    @Test
    void testGetAll() {
        given()
                .when().get("/FlexibilityEmission")
                .then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    void testGetByIdSuccess() {
        given()
                .pathParam("id", 1)
                .when().get("/FlexibilityEmission/{id}")
                .then()
                .statusCode(200)
                .body("id", is(1))
                .body("eventType", is("UNAVAILABLE_FOR_BALANCING"));
    }

    @Test
    void testGetByIdNotFound() {
        given()
                .pathParam("id", 999)
                .when().get("/FlexibilityEmission/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetByProsumerId() {
        given()
                .pathParam("prosumerId", 1)
                .when().get("/FlexibilityEmission/by-prosumer/{prosumerId}")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("id", hasItems(1, 2));
    }

    @Test
    void testGetByProsumerIdEmpty() {
        given()
                .pathParam("prosumerId", 999)
                .when().get("/FlexibilityEmission/by-prosumer/{prosumerId}")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testCreate() {
        String payload = """
                {
                  "assetId": 5,
                  "prosumerId": 5,
                  "eventType": "SELL",
                  "eventTime": "2026-05-01T10:00:00"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/FlexibilityEmission")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("eventType", is("SELL"));
    }

    @Test
    void testDeleteSuccess() {
        given()
                .pathParam("id", 1)
                .when().delete("/FlexibilityEmission/{id}")
                .then()
                .statusCode(204);

        given().pathParam("id", 1).when().get("/FlexibilityEmission/{id}").then().statusCode(404);
    }

    @Test
    void testDeleteNotFound() {
        given()
                .pathParam("id", 999)
                .when().delete("/FlexibilityEmission/{id}")
                .then()
                .statusCode(404);
    }
}
