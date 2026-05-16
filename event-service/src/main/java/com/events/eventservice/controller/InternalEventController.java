package com.events.eventservice.controller;

import com.events.eventservice.dto.CapacityRequest;
import com.events.eventservice.dto.EventAvailabilityResponse;
import com.events.eventservice.dto.EventResponse;
import com.events.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<EventAvailabilityResponse> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getAvailability(id));
    }

    @PostMapping("/{id}/reserve-capacity")
    public ResponseEntity<EventAvailabilityResponse> reserveCapacity(
            @PathVariable Long id,
            @Valid @RequestBody CapacityRequest request) {
        return ResponseEntity.ok(eventService.reserveCapacity(id, request.getQuantity()));
    }

    @PostMapping("/{id}/release-capacity")
    public ResponseEntity<EventAvailabilityResponse> releaseCapacity(
            @PathVariable Long id,
            @Valid @RequestBody CapacityRequest request) {
        return ResponseEntity.ok(eventService.releaseCapacity(id, request.getQuantity()));
    }
}
