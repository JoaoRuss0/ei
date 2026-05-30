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
        Long assetLinkId = 500L;
        String utilityOperatorName = "ArcoCegoLisbon";

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(assetLinkId, utilityOperatorName);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.createAssetLinkTopic(assetLinkId, utilityOperatorName);
        }, "Should silently handle TopicExistsException");

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(assetLinkId, utilityOperatorName);
        });

        Assertions.assertDoesNotThrow(() -> {
            topicService.deleteAssetLinkTopic(assetLinkId, utilityOperatorName);
        }, "Should silently handle UnknownTopicOrPartitionException");
    }
}