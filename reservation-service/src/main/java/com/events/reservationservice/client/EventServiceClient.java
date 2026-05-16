package com.events.reservationservice.client;

import com.events.reservationservice.dto.EventSummaryResponse;
import java.util.Optional;

public interface EventServiceClient {

    Optional<EventSummaryResponse> findEventById(Long eventId, String bearerToken);

    void reserveCapacity(Long eventId, Integer quantity, String bearerToken);

    void releaseCapacity(Long eventId, Integer quantity, String bearerToken);
}
