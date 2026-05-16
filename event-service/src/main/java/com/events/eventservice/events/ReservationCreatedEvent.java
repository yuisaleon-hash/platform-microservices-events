package com.events.eventservice.events;

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
public class ReservationCreatedEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long reservationId;
    private String userEmail;
    private Long relatedEventId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
