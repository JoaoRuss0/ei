package org.acme;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.Topic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class KafkaConsumerTracker {

    private final ConcurrentHashMap<Topic, DynamicTopicConsumer> topicConsumerThreads = new ConcurrentHashMap<>();

    public void registerTopicConsumer(Topic topic, DynamicTopicConsumer worker) {
        topicConsumerThreads.put(topic, worker);
    }

    public void stopTopicConsumer(Topic topic) {
        DynamicTopicConsumer topicConsumerThread = topicConsumerThreads.remove(topic);
        if (topicConsumerThread != null) topicConsumerThread.getConsumer().wakeup();
    }
}
