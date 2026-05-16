package com.events.eventservice.dto;

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
public class EventAvailabilityResponse {

    private Long eventId;
    private Integer availableCapacity;
    private Boolean available;
}
