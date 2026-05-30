package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.Properties;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class KafkaTopicService {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    public void createAssetLinkTopic(Long assetLinkId, String utilityOperatorName) {
        String topicName = assetLinkId.toString() + "-" + utilityOperatorName;

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            NewTopic topic = new NewTopic(topicName, Optional.empty(), Optional.empty());
            admin.createTopics(Collections.singleton(topic)).all().get();
        } catch (InterruptedException | ExecutionException e) {

            if (e.getCause() instanceof TopicExistsException) {
                return;
            }
            throw new RuntimeException("Failed to create topic " + topicName, e);
        }
    }

    public void deleteAssetLinkTopic(Long assetLinkId, String utilityOperatorName) {
        String topicName = assetLinkId.toString() + "-" + utilityOperatorName;

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            admin.deleteTopics(Collections.singleton(topicName)).all().get();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                return;
            }
            throw new RuntimeException("Failed to delete topic " + topicName, e);
        }
    }
}
