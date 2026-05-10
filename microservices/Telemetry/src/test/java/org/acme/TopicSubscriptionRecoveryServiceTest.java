package org.acme;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TopicSubscriptionRecoveryServiceTest {

    @Inject io.vertx.mutiny.mysqlclient.MySQLPool client;
    @Inject TopicSubscriptionRecoveryService recovery;
    @InjectMock KafkaDynamicConsumerTracker tracker;

    @BeforeEach
    void setup() {
        client.query("DELETE FROM TopicSubscription").execute().await().indefinitely();
        new TopicSubscription("topic-A", "failed-1").save(client).await().indefinitely();
        new TopicSubscription("topic-B", "failed-2").save(client).await().indefinitely();
        new TopicSubscription("topic-C", "alive").save(client).await().indefinitely();
    }

    @Test
    void recoverEmptyConfigDoesNothing() {
        recovery.recoverFailedServices(Optional.empty());
        verifyNoInteractions(tracker);
    }

    @Test
    void recoverPicksUpTopicsOfFailedServices() {
        recovery.recoverFailedServices(Optional.of("failed-1,failed-2"));
        verify(tracker, times(2)).track(any(), any());

        assertEquals(ServiceId.SERVICE_ID,
                TopicSubscription.findById(client, "topic-A").await().indefinitely().getOwnerService());
        assertEquals(ServiceId.SERVICE_ID,
                TopicSubscription.findById(client, "topic-B").await().indefinitely().getOwnerService());

        assertEquals("alive",
                TopicSubscription.findById(client, "topic-C").await().indefinitely().getOwnerService());
    }

    @Test
    void recoverIgnoresUnknownFailedServices() {
        recovery.recoverFailedServices(Optional.of("unknown-service"));
        verifyNoInteractions(tracker);

        assertEquals("alive",
                TopicSubscription.findById(client, "topic-C").await().indefinitely().getOwnerService());
    }
}
