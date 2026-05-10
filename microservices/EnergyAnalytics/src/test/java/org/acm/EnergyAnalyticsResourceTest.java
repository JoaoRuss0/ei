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
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class EnergyAnalyticsResourceTest {

    @Inject MySQLPool client;
    @Inject @Any InMemoryConnector connector;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM EnergyAnalytics").execute().await().indefinitely();

        connector.sink("energy-discharged-by-zone").clear();
        connector.sink("generated-energy-by-prosumer").clear();
        connector.sink("consumed-energy-by-prosumer").clear();
        connector.sink("average-soc").clear();
    }

    @Test
    void testGetEndpoint() {
        client.query("INSERT INTO EnergyAnalytics (type, entity_id, value, timestamp) VALUES ('AVERAGE_SOC', 'ZONE_A', 50.0, NOW())")
                .execute().await().indefinitely();

        given()
                .when().get("/EnergyAnalytics")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void testAnalyseEndpointExpanded() {

        String jsonPayload = """
                {
                  "assets": [
                    {"id": 1, "prosumerId": 100},
                    {"id": 2, "prosumerId": 200},
                    {"id": 3, "prosumerId": 100},
                    {"id": 4, "prosumerId": 300}
                  ],
                  "events": [
                    {
                      "asset_id": 1,
                      "asset_type": "BATTERY",
                      "grid_cell_id": "ZONE_A",
                      "timeStamp": "2026-05-10T10:00:00",
                      "Available_Energy": 100.0,
                      "Current_Output": 10.0,
                      "State_of_Charge": 80.0,
                      "Status": "ONLINE"
                    },
                    {
                      "asset_id": 1,
                      "asset_type": "BATTERY",
                      "grid_cell_id": "ZONE_A",
                      "timeStamp": "2026-05-10T11:00:00",
                      "Available_Energy": 90.0,
                      "Current_Output": 15.0,
                      "State_of_Charge": 90.0,
                      "Status": "ONLINE"
                    },
                    {
                      "asset_id": 4,
                      "asset_type": "BATTERY",
                      "grid_cell_id": "ZONE_B",
                      "timeStamp": "2026-05-10T12:00:00",
                      "Current_Output": 20.0,
                      "State_of_Charge": 50.0
                    },
                    {
                      "asset_id": 4,
                      "asset_type": "BATTERY",
                      "grid_cell_id": "ZONE_B",
                      "timeStamp": "2026-05-10T13:00:00",
                      "Current_Output": 0.0,
                      "State_of_Charge": 60.0
                    },
                    {
                      "asset_id": 2,
                      "asset_type": "SOLAR",
                      "grid_cell_id": "ZONE_B",
                      "Daily_Total": 45.5,
                      "Current_Generation": 5.0
                    },
                    {
                      "asset_id": 2,
                      "asset_type": "SOLAR",
                      "grid_cell_id": "ZONE_B",
                      "Daily_Total": 10.0,
                      "Current_Generation": 2.0
                    },
                    {
                      "asset_id": 3,
                      "asset_type": "SOLAR",
                      "grid_cell_id": "ZONE_C",
                      "Daily_Total": 100.0
                    },
                    {
                      "asset_id": 4,
                      "asset_type": "EV_CHARGER",
                      "grid_cell_id": "ZONE_A",
                      "Session_Energy": 30.0,
                      "Charging_Rate": 11.0
                    },
                    {
                      "asset_id": 4,
                      "asset_type": "EV_CHARGER",
                      "grid_cell_id": "ZONE_A",
                      "Session_Energy": 25.0,
                      "Charging_Rate": 11.0
                    },
                    {
                      "asset_id": 1,
                      "asset_type": "EV_CHARGER",
                      "grid_cell_id": "ZONE_A",
                      "Session_Energy": 15.0
                    }
                  ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonPayload)
                .when().post("/EnergyAnalytics/analyse")
                .then()
                .statusCode(200);

        InMemorySink<String> dischargedSink = connector.sink("energy-discharged-by-zone");
        InMemorySink<String> generatedSink = connector.sink("generated-energy-by-prosumer");
        InMemorySink<String> consumedSink = connector.sink("consumed-energy-by-prosumer");
        InMemorySink<String> avgSocSink = connector.sink("average-soc");

        assertEquals(2, dischargedSink.received().size(), "Should emit 2 messages (Zone A and Zone B)");
        assertEquals(2, generatedSink.received().size(), "Should emit 2 messages (Prosumer 100 and 200)");
        assertEquals(2, consumedSink.received().size(), "Should emit 2 messages (Prosumer 100 and 300)");
        assertEquals(2, avgSocSink.received().size(), "Should emit 2 messages (Zone A and Zone B)");

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    RowSet<Row> rows = client.query("SELECT type, entity_id, value FROM EnergyAnalytics").execute().await().indefinitely();

                    assertEquals(8, rows.size(), "Database should have 8 analytics records saved");

                    int validationsPassed = 0;

                    for (Row row : rows) {
                        String type = row.getString("type");
                        String entityId = row.getString("entity_id");
                        Double value = row.getDouble("value");

                        switch (type) {
                            case "ENERGY_DISCHARGED_BY_ZONE" -> {
                                if (entityId.equals("ZONE_A")) { assertEquals(25.0, value, 0.01); validationsPassed++; }
                                if (entityId.equals("ZONE_B")) { assertEquals(20.0, value, 0.01); validationsPassed++; }
                            }

                            case "ENERGY_GENERATED_BY_PROSUMER" -> {
                                if (entityId.equals("200")) { assertEquals(55.5, value, 0.01); validationsPassed++; }
                                if (entityId.equals("100")) { assertEquals(100.0, value, 0.01); validationsPassed++; }
                            }

                            case "ENERGY_CONSUMED_BY_PROSUMER" -> {
                                if (entityId.equals("300")) { assertEquals(55.0, value, 0.01); validationsPassed++; }
                                if (entityId.equals("100")) { assertEquals(15.0, value, 0.01); validationsPassed++; }
                            }

                            case "AVERAGE_SOC" -> {
                                if (entityId.equals("ZONE_A")) { assertEquals(85.0, value, 0.01); validationsPassed++; }
                                if (entityId.equals("ZONE_B")) { assertEquals(55.0, value, 0.01); validationsPassed++; }
                            }

                            default -> org.junit.jupiter.api.Assertions.fail("Unexpected analytics type found in database: " + type);
                        }
                    }

                    assertEquals(8, validationsPassed, "All 8 rows should match the exact expected calculations");
                });
    }
}
