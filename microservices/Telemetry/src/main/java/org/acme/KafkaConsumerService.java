package org.acme;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.acme.model.Topic;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class KafkaConsumerService {

    @Inject
    io.vertx.mutiny.mysqlclient.MySQLPool client;

    @Inject
    KafkaDynamicConsumerTracker tracker;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafka_servers;

    public Boolean consume(Topic topic) {

        TopicSubscription topicSubscription = TopicSubscription.findById(client, topic.getTopicName()).await().indefinitely();
        if (topicSubscription != null) return true;

        DynamicTopicConsumer worker = new DynamicTopicConsumer(topic.getTopicName(), kafka_servers, client);
        worker.start();
        tracker.track(topic, worker);

        Boolean result = new TopicSubscription(topic.getTopicName(), ServiceId.SERVICE_ID).save(client).await().indefinitely();
        if (!result) stop(topic);

        return result;
    }

    public void stop(Topic topic) {

        TopicSubscription topicSubscription = TopicSubscription.findById(client, topic.getTopicName()).await().indefinitely();
        if (topicSubscription == null || !topicSubscription.getOwnerService().equals(ServiceId.SERVICE_ID)) return;

        TopicSubscription.delete(client, topic.getTopicName());
        DynamicTopicConsumer worker = tracker.untrack(topic);
        if (worker != null) worker.getConsumer().wakeup();
    }
}
