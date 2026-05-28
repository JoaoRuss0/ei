package org.acm;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.KafkaTopicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

@QuarkusTest
class KafkaTopicServiceTest {

    @Inject KafkaTopicService topicService;

    @Test
    void testTopicLifecycle() {
        Long prosumerId = 500L;
        Long utilityOperatorId = 600L;

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(prosumerId, utilityOperatorId);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(prosumerId, utilityOperatorId);
        }, "Should silently handle TopicExistsException");

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(prosumerId, utilityOperatorId);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(prosumerId, utilityOperatorId);
        }, "Should silently handle UnknownTopicOrPartitionException");
    }
}