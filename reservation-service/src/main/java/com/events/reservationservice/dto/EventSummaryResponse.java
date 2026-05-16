package com.events.reservationservice.dto;

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
public class EventSummaryResponse {

    private Long id;
    private String title;
    private String location;
    private LocalDateTime eventDate;
    private BigDecimal price;
    private Integer availableCapacity;
    private String status;
}
