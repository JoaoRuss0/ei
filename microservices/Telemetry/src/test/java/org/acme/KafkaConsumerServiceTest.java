package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.inject.Inject;
import org.acme.model.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class KafkaConsumerServiceTest {

    final String TEST_TOPIC_NAME = "test-service-topic";
    final Topic topic = new Topic(TEST_TOPIC_NAME);

    @Inject
    KafkaConsumerService service;

    @Inject
    MySQLPool client;

    @Inject
    KafkaDynamicConsumerTracker tracker;

    @BeforeEach
    void setup() {
        client.query("CREATE TABLE IF NOT EXISTS TopicSubscription (" +
                "topic_name VARCHAR(255) PRIMARY KEY, " +
                "owner_service VARCHAR(255) NOT NULL)").execute().await().indefinitely();

        client.query("DELETE FROM TopicSubscription").execute().await().indefinitely();
    }

    @AfterEach
    void tearDown() {
        service.stop(topic);
    }

    @Test
    void testConsumeNewTopicStartsWorkerAndSavesSubscription() {
        Boolean result = service.consume(topic);

        assertTrue(result, "Consume should return true for a new topic");

        RowSet<Row> rows = client.query("SELECT * FROM TopicSubscription WHERE topic_name = '" + TEST_TOPIC_NAME + "'")
                .execute().await().indefinitely();

        assertEquals(1, rows.size(), "Database should have exactly 1 subscription");
        assertEquals(ServiceId.SERVICE_ID, rows.iterator().next().getString("owner_service"));

        DynamicTopicConsumer worker = tracker.untrack(topic);
        assertNotNull(worker, "Tracker should have returned a worker thread");

        worker.getConsumer().wakeup();
    }

    @Test
    void testConsumeExistingTopicReturnsTrueWithoutStartingNewWorker() {
        client.query("INSERT INTO TopicSubscription (topic_name, owner_service) VALUES ('" + TEST_TOPIC_NAME + "', 'some-other-service')")
                .execute().await().indefinitely();

        Boolean result = service.consume(topic);

        assertTrue(result);

        DynamicTopicConsumer worker = tracker.untrack(topic);
        assertNull(worker, "Should not have started a new worker for an already subscribed topic");
    }

    @Test
    void testStopRemovesSubscriptionAndStopsWorker() {
        service.consume(topic);

        service.stop(topic);

        RowSet<Row> rows = client.query("SELECT * FROM TopicSubscription WHERE topic_name = '" + TEST_TOPIC_NAME + "'")
                .execute().await().indefinitely();
        assertEquals(0, rows.size(), "Database record should be deleted");

        DynamicTopicConsumer worker = tracker.untrack(topic);
        assertNull(worker, "Tracker should be empty after stop() is called");
    }

    @Test
    void testStopIgnoresTopicsOwnedByOtherServices() {
        client.query("INSERT INTO TopicSubscription (topic_name, owner_service) VALUES ('" + TEST_TOPIC_NAME + "', 'DIFFERENT_SERVICE')")
                .execute().await().indefinitely();

        service.stop(topic);

        RowSet<Row> rows = client.query("SELECT * FROM TopicSubscription WHERE topic_name = '" + TEST_TOPIC_NAME + "'")
                .execute().await().indefinitely();
        assertEquals(1, rows.size(), "Database record should NOT be deleted if owned by another service");
    }
}
