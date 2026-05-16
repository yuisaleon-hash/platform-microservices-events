package com.events.reservationservice.infrastructure.client;

import com.events.reservationservice.client.EventClientErrorType;
import com.events.reservationservice.client.EventServiceClient;
import com.events.reservationservice.client.EventServiceClientException;
import com.events.reservationservice.dto.CapacityRequest;
import com.events.reservationservice.dto.EventSummaryResponse;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class WebClientEventServiceClient implements EventServiceClient {

    private final WebClient webClient;

    public WebClientEventServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${clients.event-service.base-url}") String eventServiceBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(eventServiceBaseUrl).build();
    }

    @Override
    public Optional<EventSummaryResponse> findEventById(Long eventId, String bearerToken) {
        try {
            EventSummaryResponse response = webClient.get()
                    .uri("/internal/events/{id}", eventId)
                    .headers(headers -> headers.setBearerAuth(stripBearerPrefix(bearerToken)))
                    .retrieve()
                    .bodyToMono(EventSummaryResponse.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (WebClientResponseException.NotFound exception) {
            return Optional.empty();
        } catch (WebClientRequestException exception) {
            throw new EventServiceClientException(
                    EventClientErrorType.EVENT_SERVICE_UNAVAILABLE,
                    "Event service is not available");
        } catch (WebClientResponseException exception) {
            throw new EventServiceClientException(
                    EventClientErrorType.UNEXPECTED_ERROR,
                    "Unexpected response from event service");
        }
    }

    @Override
    public void reserveCapacity(Long eventId, Integer quantity, String bearerToken) {
        postCapacity("/internal/events/{id}/reserve-capacity", eventId, quantity, bearerToken);
    }

    @Override
    public void releaseCapacity(Long eventId, Integer quantity, String bearerToken) {
        postCapacity("/internal/events/{id}/release-capacity", eventId, quantity, bearerToken);
    }

    private void postCapacity(String uri, Long eventId, Integer quantity, String bearerToken) {
        try {
            webClient.post()
                    .uri(uri, eventId)
                    .headers(headers -> headers.setBearerAuth(stripBearerPrefix(bearerToken)))
                    .bodyValue(new CapacityRequest(quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException.NotFound exception) {
            throw new EventServiceClientException(EventClientErrorType.EVENT_NOT_FOUND, "Event not found");
        } catch (WebClientResponseException.Conflict exception) {
            throw new EventServiceClientException(
                    EventClientErrorType.INSUFFICIENT_CAPACITY,
                    "Event has insufficient capacity or is not active");
        } catch (WebClientRequestException exception) {
            throw new EventServiceClientException(
                    EventClientErrorType.EVENT_SERVICE_UNAVAILABLE,
                    "Event service is not available");
        } catch (WebClientResponseException exception) {
            throw new EventServiceClientException(
                    EventClientErrorType.UNEXPECTED_ERROR,
                    "Unexpected response from event service");
        }
    }

    private String stripBearerPrefix(String bearerToken) {
        if (bearerToken == null) {
            return "";
        }
        return bearerToken.replaceFirst("(?i)^Bearer\\s+", "");
    }
}
