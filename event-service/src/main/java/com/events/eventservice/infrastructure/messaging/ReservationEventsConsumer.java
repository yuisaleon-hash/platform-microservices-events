package com.events.eventservice.infrastructure.messaging;

import com.events.eventservice.events.PaymentCompletedEvent;
import com.events.eventservice.events.ReservationCreatedEvent;
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
public class ReservationEventsConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.reservation-created}")
    public void onReservationCreated(String payload) {
        try {
            ReservationCreatedEvent event = readEvent(payload, ReservationCreatedEvent.class);
            log.info(
                    "Consumed RESERVATION_CREATED event. eventId={}, reservationId={}, relatedEventId={}, userEmail={}, quantity={}",
                    event.getEventId(),
                    event.getReservationId(),
                    event.getRelatedEventId(),
                    event.getUserEmail(),
                    event.getQuantity());
        } catch (JacksonException exception) {
            log.error("Could not deserialize RESERVATION_CREATED event. payload={}", payload, exception);
        } catch (RuntimeException exception) {
            log.error("Could not process RESERVATION_CREATED event. payload={}", payload, exception);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-completed}")
    public void onPaymentCompleted(String payload) {
        try {
            PaymentCompletedEvent event = readEvent(payload, PaymentCompletedEvent.class);
            log.info(
                    "Consumed PAYMENT_COMPLETED event. eventId={}, reservationId={}, relatedEventId={}, userEmail={}, totalAmount={}",
                    event.getEventId(),
                    event.getReservationId(),
                    event.getRelatedEventId(),
                    event.getUserEmail(),
                    event.getTotalAmount());
        } catch (JacksonException exception) {
            log.error("Could not deserialize PAYMENT_COMPLETED event. payload={}", payload, exception);
        } catch (RuntimeException exception) {
            log.error("Could not process PAYMENT_COMPLETED event. payload={}", payload, exception);
        }
    }

    private <T> T readEvent(String payload, Class<T> eventType) throws JacksonException {
        return objectMapper.readValue(payload, eventType);
    }
}
