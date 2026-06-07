package org.acme;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import org.acme.model.Topic;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class TelemetryResourceTest {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @InjectMock
    KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setupData() {
        client.query("DELETE FROM Telemetry").execute().await().indefinitely();

        List<Tuple> batch = new ArrayList<>();
        // Asset 1 in cell-A (3 events)
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 10, 0), 1L, "cell-A", "BATTERY", "ONLINE", null));
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 11, 0), 1L, "cell-A", "BATTERY", "ONLINE", null));
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 12, 0), 1L, "cell-A", "BATTERY", "ONLINE", null));
        // Asset 2 in cell-A (1 event)
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 1, 13, 0), 2L, "cell-A", "SOLAR", "OK", null));
        // Asset 3 in cell-B (2 events)
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 2, 9, 0), 3L, "cell-B", "EV_CHARGER", "OK", "CHARGING"));
        batch.add(Tuple.of(LocalDateTime.of(2026, 1, 2, 10, 0), 3L, "cell-B", "EV_CHARGER", "OK", "PLUGGED"));

        client.preparedQuery("INSERT INTO Telemetry (timeStamp, asset_id, grid_cell_id, asset_type, Status, Plug_Status) VALUES (?, ?, ?, ?, ?, ?)")
                .executeBatch(batch).await().indefinitely();
    }

    @Test
    void testGetAll() {
        given()
                .when().get("/Telemetry")
                .then()
                .statusCode(200)
                .body("size()", is(6));
    }

    @Test
    void testGetByAssetId() {
        given()
                .pathParam("assetId", 1)
                .when().get("/Telemetry/by-asset/{assetId}")
                .then()
                .statusCode(200)
                .body("size()", is(3))
                .body("asset_id", everyItem(equalTo(1)));
    }

    @Test
    void testGetByAssetIdEmpty() {
        given()
                .pathParam("assetId", 999)
                .when().get("/Telemetry/by-asset/{assetId}")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testGetByGridCellId() {
        given()
                .pathParam("gridCellId", "cell-A")
                .when().get("/Telemetry/by-grid-cell/{gridCellId}")
                .then()
                .statusCode(200)
                .body("size()", is(4));
    }

    @Test
    void testGetByGridCellIds() {
        given()
                .queryParam("gridCellIds", "cell-A")
                .queryParam("gridCellIds", "cell-B")
                .when().get("/Telemetry/by-grid-cell-ids/")
                .then()
                .statusCode(200)
                .body("size()", is(6));
    }

    @Test
    void testGetByAssetIds() {
        given()
                .queryParam("assetIds", 1)
                .queryParam("assetIds", 3)
                .when().get("/Telemetry/by-asset-ids/")
                .then()
                .statusCode(200)
                .body("size()", is(5));
    }

    @Test
    void testConsumeDelegatesToService() {
        Mockito.when(kafkaConsumerService.consume(any(Topic.class))).thenReturn(true);

        given()
                .contentType(ContentType.JSON)
                .body("{\"topicName\":\"test-topic\"}")
                .when().post("/Telemetry/consume")
                .then()
                .statusCode(200)
                .body(equalTo("New worker started"));

        Mockito.verify(kafkaConsumerService).consume(new Topic("test-topic"));
    }

    @Test
    void testStopDelegatesToService() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"topicName\":\"test-topic\"}")
                .when().post("/Telemetry/stop")
                .then()
                .statusCode(204);

        Mockito.verify(kafkaConsumerService).stop(new Topic("test-topic"));
    }

    @Test
    void testDeleteByAssetIdRemovesAllRowsForAsset() {
        given()
                .pathParam("assetId", 1)
                .when().delete("/Telemetry/by-asset/{assetId}")
                .then()
                .statusCode(200)
                .body("deleted", is(3));

        given()
                .pathParam("assetId", 1)
                .when().get("/Telemetry/by-asset/{assetId}")
                .then()
                .statusCode(200)
                .body("size()", is(0));

        given()
                .when().get("/Telemetry")
                .then()
                .statusCode(200)
                .body("size()", is(3));
    }

    @Test
    void testDeleteByAssetIdMissingReturnsZero() {
        given()
                .pathParam("assetId", 999)
                .when().delete("/Telemetry/by-asset/{assetId}")
                .then()
                .statusCode(200)
                .body("deleted", is(0));

        given()
                .when().get("/Telemetry")
                .then()
                .statusCode(200)
                .body("size()", is(6));
    }
}
