package com.events.authservice.messaging;

public interface EventPublisher {

    void publish(String topic, Object event);
}
