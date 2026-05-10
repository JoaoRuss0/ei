package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FlexibilityEmissionResourceTest {

    @Inject @Any
    InMemoryConnector connector;

    @BeforeAll
    static void switchChannels() {
        InMemoryConnector.switchOutgoingChannelsToInMemory("flexibility-offers");
    }

    @AfterAll
    static void revert() {
        InMemoryConnector.clear();
    }

    @BeforeEach
    void clearSink() {
        InMemorySink<String> sink = connector.sink("flexibility-offers");
        sink.clear();
    }

    @Test
    void getReturnsList() {
        given()
                .when().get("/FlexibilityEmission")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void evaluateEmitsSellOnHighSocDuringPeak() {
        String body = """
            {
              "prosumer_id": 1,
              "events": [{
                "id": 1,
                "timeStamp": "2026-01-01T19:00:00",
                "asset_id": 1,
                "asset_type": "BATTERY",
                "grid_cell_id": "PORTO_NORTH",
                "State_of_Charge": 0.95,
                "Status": "ONLINE"
              }],
              "cells": [{
                "id": "PORTO_NORTH",
                "peakHoursStartTime": "2026-01-01T18:00:00",
                "peakHoursEndTime": "2026-01-01T21:00:00"
              }]
            }
            """;

        given()
                .contentType("application/json")
                .body(body)
                .when().post("/FlexibilityEmission/evaluate")
                .then().statusCode(200);

        InMemorySink<String> sink = connector.sink("flexibility-offers");
        assertEquals(1, sink.received().size());
        assertTrue(sink.received().get(0).getPayload().contains("SELL"));
    }

    @Test
    void evaluateEmitsUnavailableOnLowSoc() {
        String body = """
            {
              "prosumer_id": 1,
              "events": [{
                "id": 2,
                "timeStamp": "2026-01-01T14:00:00",
                "asset_id": 2,
                "asset_type": "BATTERY",
                "grid_cell_id": "PORTO_NORTH",
                "State_of_Charge": 0.1,
                "Status": "ONLINE"
              }],
              "cells": [{
                "id": "PORTO_NORTH",
                "peakHoursStartTime": "2026-01-01T18:00:00",
                "peakHoursEndTime": "2026-01-01T21:00:00"
              }]
            }
            """;

        given()
                .contentType("application/json")
                .body(body)
                .when().post("/FlexibilityEmission/evaluate")
                .then().statusCode(200);

        InMemorySink<String> sink = connector.sink("flexibility-offers");
        assertEquals(1, sink.received().size());
        assertTrue(sink.received().get(0).getPayload().contains("UNAVAILABLE"));
    }
}
