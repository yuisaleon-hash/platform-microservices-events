package com.events.reservationservice.infrastructure.messaging;

import com.events.reservationservice.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class UserEventsConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.user-registered}")
    public void onUserRegistered(String payload) {
        try {
            UserRegisteredEvent event = readEvent(payload);
            log.info(
                    "Consumed USER_REGISTERED event. eventId={}, userId={}, email={}, username={}, role={}",
                    event.getEventId(),
                    event.getUserId(),
                    event.getEmail(),
                    event.getUsername(),
                    event.getRole());
        } catch (JacksonException exception) {
            log.error("Could not deserialize USER_REGISTERED event. payload={}", payload, exception);
        } catch (RuntimeException exception) {
            log.error("Could not process USER_REGISTERED event. payload={}", payload, exception);
        }
    }

    private UserRegisteredEvent readEvent(String payload) throws JacksonException {
        return objectMapper.readValue(payload, UserRegisteredEvent.class);
    }
}
