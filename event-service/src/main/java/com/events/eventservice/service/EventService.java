package com.events.eventservice.service;

import com.events.eventservice.dto.EventAvailabilityResponse;
import com.events.eventservice.dto.EventRequest;
import com.events.eventservice.dto.EventResponse;
import com.events.eventservice.entity.Event;
import com.events.eventservice.entity.EventStatus;
import com.events.eventservice.repository.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .price(request.getPrice())
                .totalCapacity(request.getTotalCapacity())
                .availableCapacity(request.getAvailableCapacity())
                .status(request.getStatus())
                .build();

        return toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = findEventById(id);
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public EventAvailabilityResponse getAvailability(Long id) {
        Event event = findEventById(id);
        return toAvailabilityResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findEventById(id);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setPrice(request.getPrice());
        event.setTotalCapacity(request.getTotalCapacity());
        event.setAvailableCapacity(request.getAvailableCapacity());
        event.setStatus(request.getStatus());

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = findEventById(id);
        eventRepository.delete(event);
    }

    @Transactional
    public EventAvailabilityResponse reserveCapacity(Long id, Integer quantity) {
        Event event = findEventById(id);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not active");
        }

        if (event.getAvailableCapacity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient event capacity");
        }

        event.setAvailableCapacity(event.getAvailableCapacity() - quantity);
        if (event.getAvailableCapacity() == 0) {
            event.setStatus(EventStatus.SOLD_OUT);
        }

        return toAvailabilityResponse(eventRepository.save(event));
    }

    @Transactional
    public EventAvailabilityResponse releaseCapacity(Long id, Integer quantity) {
        Event event = findEventById(id);
        int restoredCapacity = Math.min(event.getAvailableCapacity() + quantity, event.getTotalCapacity());
        event.setAvailableCapacity(restoredCapacity);

        if (event.getStatus() == EventStatus.SOLD_OUT && restoredCapacity > 0) {
            event.setStatus(EventStatus.ACTIVE);
        }

        return toAvailabilityResponse(eventRepository.save(event));
    }

    private Event findEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .price(event.getPrice())
                .totalCapacity(event.getTotalCapacity())
                .availableCapacity(event.getAvailableCapacity())
                .status(event.getStatus())
                .build();
    }

    private EventAvailabilityResponse toAvailabilityResponse(Event event) {
        return EventAvailabilityResponse.builder()
                .eventId(event.getId())
                .availableCapacity(event.getAvailableCapacity())
                .available(event.getStatus() == EventStatus.ACTIVE && event.getAvailableCapacity() > 0)
                .build();
    }
}
