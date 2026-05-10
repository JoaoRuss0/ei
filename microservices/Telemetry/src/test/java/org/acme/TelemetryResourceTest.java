package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TelemetryResourceTest {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @BeforeEach
    void setupData() {
        client.query("DELETE FROM Telemetry").execute().await().indefinitely();

        List<Tuple> batch = new ArrayList<>();

        // --- DATA FOR 'AROUND TIMESTAMP' TEST (Asset 1) ---
        // Target timestamp for the test will be: 2026-01-01T19:00:00
        // Insert 15 events BEFORE the target timestamp (18:10 to 18:24)
        for (int i = 10; i < 25; i++) {
            batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 18, i), 1L, "cell-1", "EV", "OK", "PLUGGED"));
        }
        // Insert 15 events AFTER the target timestamp (19:10 to 19:24)
        for (int i = 10; i < 25; i++) {
            batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 19, i), 1L, "cell-1", "EV", "OK", "PLUGGED"));
        }

        // --- DATA FOR 'LATEST' ENDPOINT TEST (Asset 2) ---
        // Insert an older event
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 2, 10, 0), 2L, "cell-2", "SOLAR", "OK", "UNPLUGGED"));
        // Insert the LATEST event
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 2, 12, 0), 2L, "cell-2", "SOLAR", "OK", "UNPLUGGED"));

        // --- DATA FOR 'LAST HOUR' TEST (Asset 3) ---
        batch.add(Tuple.of(LocalDateTime.now(), 3L, "cell-3", "WIND", "OK", "UNPLUGGED"));

        client.preparedQuery("INSERT INTO Telemetry (timeStamp, asset_id, grid_cell_id, asset_type, Status, Plug_Status) VALUES (?, ?, ?, ?, ?, ?)")
                .executeBatch(batch).await().indefinitely();
    }

    @Test
    void getReturnsList() {
        given()
                .when().get("/Telemetry")
                .then()
                .statusCode(200)
                .body("size()", is(33));
    }

    @Test
    void getLastHourReturnsList() {
        given()
                .when().get("/Telemetry/last-hour")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].asset_id", equalTo(3));
    }

    @Test
    void getAroundTimestampReturnsList() {
        given()
                .pathParam("assetId", 1)
                .pathParam("timestamp", "2026-01-01T19:00:00")
                .when().get("/Telemetry/asset/{assetId}/around/{timestamp}/")
                .then()
                .statusCode(200)
                .body("size()", is(20))
                .body("asset_id", everyItem(equalTo(1)));
    }

    @Test
    void getLatestReturnsList() {
        String jsonPayload = "[{\"assetId\": 2, \"gridCellId\": \"cell-2\"}]";

        given()
                .contentType(ContentType.JSON)
                .body(jsonPayload)
                .when().post("/Telemetry/latest")
                .then()
                .statusCode(200)
                // Even though there are two events for Asset 2, it should only return the latest one
                .body("size()", is(1))
                .body("[0].asset_id", equalTo(2))
                // Verify it grabbed the 12:00:00 event, not the 10:00:00 event
                .body("[0].timeStamp", equalTo("2026-01-02T12:00:00"));
    }
}