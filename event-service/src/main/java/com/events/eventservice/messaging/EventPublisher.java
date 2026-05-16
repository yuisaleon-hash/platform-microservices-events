package com.events.eventservice.messaging;

public interface EventPublisher {

    void publish(String topic, Object event);
}
