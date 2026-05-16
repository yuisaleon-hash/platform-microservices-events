package com.events.reservationservice.infrastructure.messaging;

import com.events.reservationservice.messaging.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEventPublisher implements EventPublisher {

    @Override
    public void publish(String topic, Object event) {
        // Kafka publication is disabled for this runtime.
    }
}
