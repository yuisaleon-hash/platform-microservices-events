package com.events.reservationservice.messaging;

public interface EventPublisher {

    void publish(String topic, Object event);
}
