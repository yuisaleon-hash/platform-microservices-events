package com.events.authservice.service;

import com.events.authservice.dto.AuthResponse;
import com.events.authservice.dto.LoginRequest;
import com.events.authservice.dto.RegisterRequest;
import com.events.authservice.entity.Role;
import com.events.authservice.entity.User;
import com.events.authservice.events.UserRegisteredEvent;
import com.events.authservice.messaging.EventPublisher;
import com.events.authservice.repository.UserRepository;
import com.events.authservice.security.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private static final String USER_REGISTERED_EVENT_TYPE = "USER_REGISTERED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EventPublisher eventPublisher;
    private final String userRegisteredTopic;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EventPublisher eventPublisher,
            @Value("${app.kafka.topics.user-registered}") String userRegisteredTopic) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.userRegisteredTopic = userRegisteredTopic;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        publishUserRegisteredEvent(savedUser);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    private void publishUserRegisteredEvent(User user) {
        LocalDateTime occurredAt = LocalDateTime.now();
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(USER_REGISTERED_EVENT_TYPE)
                .occurredAt(occurredAt)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .registeredAt(occurredAt)
                .build();

        try {
            eventPublisher.publish(userRegisteredTopic, event);
        } catch (RuntimeException exception) {
            log.error("Could not publish USER_REGISTERED event. userId={}, email={}",
                    user.getId(),
                    user.getEmail(),
                    exception);
        }
    }
}
