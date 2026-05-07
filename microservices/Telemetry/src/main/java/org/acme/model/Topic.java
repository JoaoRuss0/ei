package org.acme.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Topic {
    private String topicName;

    public Topic() {
    }

    public Topic(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public String toString() {
        return "Topic [TopicName=" + topicName + "]";
    }
}
