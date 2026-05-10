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
        Long assetId = 500L;
        String gridCellId = "TEST_ZONE";

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(assetId, gridCellId);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(assetId, gridCellId);
        }, "Should silently handle TopicExistsException");

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(assetId, gridCellId);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(assetId, gridCellId);
        }, "Should silently handle UnknownTopicOrPartitionException");
    }
}