package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DynamicTopicConsumerTest {

    @Inject
    MySQLPool client;

    MockConsumer<String, String> mockConsumer;
    DynamicTopicConsumer consumerThread;

    final String TEST_TOPIC = "telemetry-test-topic";

    @BeforeEach
    void setup() {
        client.query("DELETE FROM Telemetry").execute().await().indefinitely();
        mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    }

    @AfterEach
    void tearDown() {
        if (consumerThread != null && consumerThread.getConsumer() != null) {
            consumerThread.getConsumer().wakeup();
        }
    }

    @Test
    void testBatteryTelemetryConsumption() throws Exception {

        String batteryJson = """
                {
                    "timeStamp": "2026-05-10T12:00:00",
                    "asset_type": "BATTERY",
                    "asset_id": "1001",
                    "grid_cell_id": "cell-abc",
                    "payload": {
                        "soc_percent": 85.5,
                        "energy_available_kwh": 150.0,
                        "active_power_kw": 50.0,
                        "max_discharge_power_kw": 100.0,
                        "soh_percent": 98.0,
                        "connection_status": "ONLINE"
                    }
                }
                """;

        TopicPartition partition = new TopicPartition(TEST_TOPIC, 0);

        mockConsumer.schedulePollTask(() -> {
            mockConsumer.rebalance(Collections.singletonList(partition));

            HashMap<TopicPartition, Long> startingOffsets = new HashMap<>();
            startingOffsets.put(partition, 0L);
            mockConsumer.updateBeginningOffsets(startingOffsets);

            mockConsumer.addRecord(new ConsumerRecord<>(TEST_TOPIC, 0, 0L, "1001", batteryJson));
        });

        consumerThread = new DynamicTopicConsumer(TEST_TOPIC, mockConsumer, client);
        consumerThread.start();

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    io.vertx.mutiny.sqlclient.RowSet<io.vertx.mutiny.sqlclient.Row> rows =
                            client.query("SELECT * FROM Telemetry").execute().await().indefinitely();

                    assertEquals(1, rows.size(), "Database should have exactly 1 record for asset 1001");

                    io.vertx.mutiny.sqlclient.Row row = rows.iterator().next();
                    assertEquals("BATTERY", row.getString("asset_type"));
                    assertEquals(85.5f, row.getFloat("State_of_Charge"));
                    assertEquals("ONLINE", row.getString("Status"));
                });
    }
}