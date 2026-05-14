package com.events.eventservice.service;

import com.events.eventservice.dto.EventRequest;
import com.events.eventservice.dto.EventResponse;
import com.events.eventservice.entity.Event;
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
}
