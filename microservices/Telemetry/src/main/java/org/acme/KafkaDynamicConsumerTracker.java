package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.Topic;

import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class KafkaDynamicConsumerTracker {

    private final ConcurrentHashMap<Topic, DynamicTopicConsumer> trackedConsumers = new ConcurrentHashMap<>();

    public void track(Topic topic, DynamicTopicConsumer consumer) {
        trackedConsumers.put(topic, consumer);
    }

    public DynamicTopicConsumer untrack(Topic topic) {
        return trackedConsumers.remove(topic);
    }
}
