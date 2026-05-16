package com.events.reservationservice.client;

import lombok.Getter;

@Getter
public class EventServiceClientException extends RuntimeException {

    private final EventClientErrorType errorType;

    public EventServiceClientException(EventClientErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
}
