package com.events.reservationservice.service;

import com.events.reservationservice.dto.PaymentRequest;
import com.events.reservationservice.dto.PaymentResponse;
import com.events.reservationservice.dto.ReservationRequest;
import com.events.reservationservice.dto.ReservationResponse;
import com.events.reservationservice.entity.Reservation;
import com.events.reservationservice.entity.ReservationStatus;
import com.events.reservationservice.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final BigDecimal TEMPORARY_UNIT_PRICE = new BigDecimal("100.00");

    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse createReservation(String userEmail, ReservationRequest request) {
        Reservation reservation = Reservation.builder()
                .userEmail(userEmail)
                .eventId(request.getEventId())
                .quantity(request.getQuantity())
                .totalAmount(TEMPORARY_UNIT_PRICE.multiply(BigDecimal.valueOf(request.getQuantity())))
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
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

        return toPaymentResponse(reservationRepository.save(reservation), "Payment completed successfully");
    }

    @Transactional
    public ReservationResponse cancelReservation(String userEmail, Long id) {
        Reservation reservation = findOwnedReservation(userEmail, id);

        if (reservation.getStatus() == ReservationStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se cancelan reservas pagadas");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        return toResponse(reservationRepository.save(reservation));
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
}
