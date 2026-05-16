package com.events.reservationservice.service;

import com.events.reservationservice.client.EventClientErrorType;
import com.events.reservationservice.client.EventServiceClient;
import com.events.reservationservice.client.EventServiceClientException;
import com.events.reservationservice.dto.EventSummaryResponse;
import com.events.reservationservice.dto.PaymentRequest;
import com.events.reservationservice.dto.PaymentResponse;
import com.events.reservationservice.dto.ReservationRequest;
import com.events.reservationservice.dto.ReservationResponse;
import com.events.reservationservice.entity.Reservation;
import com.events.reservationservice.entity.ReservationStatus;
import com.events.reservationservice.events.PaymentCompletedEvent;
import com.events.reservationservice.events.ReservationCreatedEvent;
import com.events.reservationservice.messaging.EventPublisher;
import com.events.reservationservice.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ReservationService {

    private static final String ACTIVE_EVENT_STATUS = "ACTIVE";
    private static final String RESERVATION_CREATED_EVENT_TYPE = "RESERVATION_CREATED";
    private static final String PAYMENT_COMPLETED_EVENT_TYPE = "PAYMENT_COMPLETED";

    private final ReservationRepository reservationRepository;
    private final EventServiceClient eventServiceClient;
    private final EventPublisher eventPublisher;
    private final String reservationCreatedTopic;
    private final String paymentCompletedTopic;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventServiceClient eventServiceClient,
            EventPublisher eventPublisher,
            @Value("${app.kafka.topics.reservation-created}") String reservationCreatedTopic,
            @Value("${app.kafka.topics.payment-completed}") String paymentCompletedTopic) {
        this.reservationRepository = reservationRepository;
        this.eventServiceClient = eventServiceClient;
        this.eventPublisher = eventPublisher;
        this.reservationCreatedTopic = reservationCreatedTopic;
        this.paymentCompletedTopic = paymentCompletedTopic;
    }

    @Transactional
    public ReservationResponse createReservation(String userEmail, String bearerToken, ReservationRequest request) {
        EventSummaryResponse event = findEventForReservation(request.getEventId(), bearerToken);

        validateEventCanBeReserved(event, request.getQuantity());
        reserveEventCapacity(request.getEventId(), request.getQuantity(), bearerToken);

        Reservation reservation = Reservation.builder()
                .userEmail(userEmail)
                .eventId(request.getEventId())
                .quantity(request.getQuantity())
                .totalAmount(event.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .status(ReservationStatus.PENDING)
                .build();

        try {
            Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
            publishReservationCreatedEvent(savedReservation);
            return toResponse(savedReservation);
        } catch (RuntimeException exception) {
            compensateReservedCapacity(request.getEventId(), request.getQuantity(), bearerToken);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String userEmail) {
        return reservationRepository.findByUserEmail(userEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(String userEmail, Long id) {
        return toResponse(findOwnedReservation(userEmail, id));
    }

    @Transactional
    public PaymentResponse payReservation(String userEmail, Long id, PaymentRequest request) {
        Reservation reservation = findOwnedReservation(userEmail, id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede pagar una reserva cancelada");
        }

        if (reservation.getStatus() == ReservationStatus.PAID) {
            return toPaymentResponse(reservation, "Reservation was already paid");
        }

        reservation.setStatus(ReservationStatus.PAID);
        reservation.setPaidAt(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        publishPaymentCompletedEvent(savedReservation);

        return toPaymentResponse(savedReservation, "Payment completed successfully");
    }

    @Transactional
    public ReservationResponse cancelReservation(String userEmail, String bearerToken, Long id) {
        Reservation reservation = findOwnedReservation(userEmail, id);

        if (reservation.getStatus() == ReservationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se cancelan reservas pagadas");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        releaseEventCapacity(reservation.getEventId(), reservation.getQuantity(), bearerToken);

        return toResponse(savedReservation);
    }

    private EventSummaryResponse findEventForReservation(Long eventId, String bearerToken) {
        try {
            return eventServiceClient.findEventById(eventId, bearerToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        } catch (EventServiceClientException exception) {
            throw mapEventClientException(exception);
        }
    }

    private void validateEventCanBeReserved(EventSummaryResponse event, Integer quantity) {
        if (!ACTIVE_EVENT_STATUS.equals(event.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not active");
        }

        if (event.getAvailableCapacity() == null || event.getAvailableCapacity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient event capacity");
        }
    }

    private void compensateReservedCapacity(Long eventId, Integer quantity, String bearerToken) {
        try {
            eventServiceClient.releaseCapacity(eventId, quantity, bearerToken);
        } catch (EventServiceClientException exception) {
            throw mapEventClientException(exception);
        }
    }

    private void reserveEventCapacity(Long eventId, Integer quantity, String bearerToken) {
        try {
            eventServiceClient.reserveCapacity(eventId, quantity, bearerToken);
        } catch (EventServiceClientException exception) {
            throw mapEventClientException(exception);
        }
    }

    private void releaseEventCapacity(Long eventId, Integer quantity, String bearerToken) {
        try {
            eventServiceClient.releaseCapacity(eventId, quantity, bearerToken);
        } catch (EventServiceClientException exception) {
            throw mapEventClientException(exception);
        }
    }

    private ResponseStatusException mapEventClientException(EventServiceClientException exception) {
        if (exception.getErrorType() == EventClientErrorType.EVENT_NOT_FOUND) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }

        if (exception.getErrorType() == EventClientErrorType.EVENT_SERVICE_UNAVAILABLE) {
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }

        if (exception.getErrorType() == EventClientErrorType.INSUFFICIENT_CAPACITY
                || exception.getErrorType() == EventClientErrorType.EVENT_NOT_ACTIVE) {
            return new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, exception.getMessage(), exception);
    }

    private Reservation findOwnedReservation(String userEmail, Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (!reservation.getUserEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reservation does not belong to authenticated user");
        }

        return reservation;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .userEmail(reservation.getUserEmail())
                .eventId(reservation.getEventId())
                .quantity(reservation.getQuantity())
                .totalAmount(reservation.getTotalAmount())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .paidAt(reservation.getPaidAt())
                .cancelledAt(reservation.getCancelledAt())
                .build();
    }

    private PaymentResponse toPaymentResponse(Reservation reservation, String message) {
        return PaymentResponse.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .totalAmount(reservation.getTotalAmount())
                .paidAt(reservation.getPaidAt())
                .message(message)
                .build();
    }

    private void publishReservationCreatedEvent(Reservation reservation) {
        LocalDateTime occurredAt = LocalDateTime.now();
        ReservationCreatedEvent event = ReservationCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(RESERVATION_CREATED_EVENT_TYPE)
                .occurredAt(occurredAt)
                .reservationId(reservation.getId())
                .userEmail(reservation.getUserEmail())
                .relatedEventId(reservation.getEventId())
                .quantity(reservation.getQuantity())
                .totalAmount(reservation.getTotalAmount())
                .createdAt(reservation.getCreatedAt())
                .build();

        publishEvent(reservationCreatedTopic, event, RESERVATION_CREATED_EVENT_TYPE, reservation.getId());
    }

    private void publishPaymentCompletedEvent(Reservation reservation) {
        LocalDateTime occurredAt = LocalDateTime.now();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(PAYMENT_COMPLETED_EVENT_TYPE)
                .occurredAt(occurredAt)
                .reservationId(reservation.getId())
                .relatedEventId(reservation.getEventId())
                .userEmail(reservation.getUserEmail())
                .totalAmount(reservation.getTotalAmount())
                .paidAt(reservation.getPaidAt())
                .build();

        publishEvent(paymentCompletedTopic, event, PAYMENT_COMPLETED_EVENT_TYPE, reservation.getId());
    }

    private void publishEvent(String topic, Object event, String eventType, Long reservationId) {
        try {
            eventPublisher.publish(topic, event);
        } catch (RuntimeException exception) {
            log.error("Could not publish {} event. reservationId={}",
                    eventType,
                    reservationId,
                    exception);
        }
    }
}
