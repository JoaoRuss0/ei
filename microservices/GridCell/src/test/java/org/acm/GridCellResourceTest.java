package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.inject.Inject;
import org.acme.GridCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class GridCellResourceTest {

    @Inject MySQLPool client;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM GridCell").execute()
                .flatMap(r -> client.query("INSERT INTO GridCell (id, address, postal_code, peak_hours_start, peak_hours_end, max_load, x_coords, y_coords, operator_id) VALUES ('PORTO_NORTH', 'Rua do Figo', '2222-232', '2026-01-01 18:00:00', '2026-01-01 21:00:00', 10, 1, 1, 1)").execute())
                .flatMap(r -> client.query("INSERT INTO GridCell (id, address, postal_code, peak_hours_start, peak_hours_end, max_load, x_coords, y_coords, operator_id) VALUES ('LISBON_SOUTH', 'Rua da Pera', '3142-521', '2026-01-01 09:30:00', '2026-01-01 12:00:00', 50, 1, 0, 2)").execute())
                .await().indefinitely();
    }

    @Test
    void testFindAll() {
        given()
                .when().get("/GridCell")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("id", hasItems("PORTO_NORTH", "LISBON_SOUTH"));
    }

    @Test
    void testFindByIdSuccess() {
        given()
                .pathParam("id", "PORTO_NORTH")
                .when().get("/GridCell/{id}")
                .then()
                .statusCode(200)
                .body("id", is("PORTO_NORTH"))
                .body("address", is("Rua do Figo"));
    }

    @Test
    void testFindByIdNotFound() {
        given()
                .pathParam("id", "NON_EXISTENT_ID")
                .when().get("/GridCell/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateSuccess() {
        GridCell newCell = new GridCell(
                "COIMBRA_01", "Rua da Sofia", "3000-001",
                LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                100L, 10L, 5L, 5L
        );

        given()
                .contentType(ContentType.JSON)
                .body(newCell)
                .when().post("/GridCell")
                .then()
                .statusCode(201)
                .header("Location", containsString("/GridCell/COIMBRA_01"));
    }

    @Test
    void testUpdateSuccess() {
        String updatePayload = """
                {
                    "address": "Updated Lisbon Ave",
                    "postalCode": "1000-001",
                    "peakHoursStartTime": "2026-01-01T10:00:00",
                    "peakHoursEndTime": "2026-01-01T12:00:00",
                    "maxLoad": 75,
                    "operatorId": 2,
                    "xCoords": 1,
                    "yCoords": 0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .pathParam("id", "LISBON_SOUTH")
                .when().put("/GridCell/{id}")
                .then()
                .statusCode(204);

        given()
                .pathParam("id", "LISBON_SOUTH")
                .when().get("/GridCell/{id}")
                .then()
                .body("address", is("Updated Lisbon Ave"))
                .body("maxLoad", is(75));
    }

    @Test
    void testUpdateNotFound() {
        String updatePayload = """
                { "address": "Any", "postalCode": "000", "maxLoad": 10, "operatorId": 1, "xCoords": 9, "yCoords": 9 }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .pathParam("id", "GHOST_ID")
                .when().put("/GridCell/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteSuccess() {
        given()
                .pathParam("id", "PORTO_NORTH")
                .when().delete("/GridCell/{id}")
                .then()
                .statusCode(204);

        given()
                .pathParam("id", "PORTO_NORTH")
                .when().get("/GridCell/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteNotFound() {
        given()
                .pathParam("id", "NOT_THERE")
                .when().delete("/GridCell/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetByOperatorIds() {
        given()
                .queryParam("operatorIds", 1)
                .queryParam("operatorIds", 2)
                .when().get("/GridCell/by-operator-ids")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("operatorId", hasItems(1, 2));
    }

    @Test
    void testGetByOperatorIdNoResult() {
        given()
                .queryParam("operatorIds", 999)
                .when().get("/GridCell/by-operator-ids")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }
}