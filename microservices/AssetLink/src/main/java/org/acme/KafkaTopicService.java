package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.errors.TopicExistsException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class KafkaTopicService {

    private static final short REPLICATION_FACTOR = 1;
    private static final int PARTITON_COUNT = 1;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    public void createAssetLinkTopic(Long assetLinkId, String utilityOperator) {
        String topicName = assetLinkId + "-" + utilityOperator;

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            NewTopic topic = new NewTopic(topicName, PARTITON_COUNT, REPLICATION_FACTOR);
            admin.createTopics(Collections.singleton(topic)).all().get();
        } catch (InterruptedException | ExecutionException e) {

            if (e.getCause() instanceof TopicExistsException) {
                return;
            }
            throw new RuntimeException("Failed to create topic " + topicName, e);
        }
    }
}
