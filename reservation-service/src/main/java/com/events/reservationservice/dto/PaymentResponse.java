package com.events.reservationservice.dto;

import com.events.reservationservice.entity.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long reservationId;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;
    private String message;
}
