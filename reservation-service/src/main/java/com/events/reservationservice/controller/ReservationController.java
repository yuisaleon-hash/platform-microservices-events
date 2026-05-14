package com.events.reservationservice.controller;

import com.events.reservationservice.dto.PaymentRequest;
import com.events.reservationservice.dto.PaymentResponse;
import com.events.reservationservice.dto.ReservationRequest;
import com.events.reservationservice.dto.ReservationResponse;
import com.events.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            Authentication authentication,
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(getAuthenticatedEmail(authentication), request);
        return ResponseEntity
                .created(URI.create("/reservations/" + response.getId()))
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getMyReservations(getAuthenticatedEmail(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(getAuthenticatedEmail(authentication), id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payReservation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(reservationService.payReservation(getAuthenticatedEmail(authentication), id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(getAuthenticatedEmail(authentication), id));
    }

    private String getAuthenticatedEmail(Authentication authentication) {
        return authentication.getName();
    }
}
