package com.events.authservice.infrastructure.messaging;

import com.events.authservice.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, Object event) {
        try {
            kafkaTemplate.send(topic, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Kafka publish failed. topic={}, eventClass={}",
                                    topic,
                                    event.getClass().getSimpleName(),
                                    exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Kafka publish could not be scheduled. topic={}, eventClass={}",
                    topic,
                    event.getClass().getSimpleName(),
                    exception);
        }
    }
}
