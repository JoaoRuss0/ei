package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.acme.model.Topic;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TopicSubscriptionRecoveryService {

    @ConfigProperty(name = "recovery.failed-service-uuids", defaultValue = "")
    Optional<String> failedUuidsConfig;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafkaServers;

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    KafkaDynamicConsumerTracker tracker;

    void onStartup(@Observes StartupEvent ev) {
        recoverFailedServices(failedUuidsConfig);
    }

    public void recoverFailedServices(Optional<String> failedUuidsConfig) {
        if (failedUuidsConfig.isEmpty() || failedUuidsConfig.get().isBlank()) return;

        String failedUuids = failedUuidsConfig.get();
        List<String> failedIds = Arrays.stream(failedUuids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (failedIds.isEmpty()) return;

        String placeholders = failedIds.stream().map(s -> "?").collect(Collectors.joining(","));
        String query = "SELECT topic_name FROM TopicSubscription WHERE owner_service IN (" + placeholders + ")";

        Tuple params = Tuple.tuple();
        failedIds.forEach(params::addString);

        List<String> topics = new ArrayList<>();
        client.preparedQuery(query).execute(params)
                .onItem().invoke(rows -> rows.forEach(r -> topics.add(r.getString("topic_name"))))
                .await().indefinitely();

        for (String topic : topics) takeOver(topic);
    }

    private void takeOver(String topicName) {
        TopicSubscription.updateOwnerService(client, topicName, ServiceId.SERVICE_ID).await().indefinitely();
        DynamicTopicConsumer worker = new DynamicTopicConsumer(topicName, kafkaServers, client);
        worker.start();
        tracker.track(new Topic(topicName), worker);
    }
}
