package com.events.authservice.events;

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
public class UserRegisteredEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime registeredAt;
}
