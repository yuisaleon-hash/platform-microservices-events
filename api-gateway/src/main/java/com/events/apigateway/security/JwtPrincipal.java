package com.events.apigateway.security;

public record JwtPrincipal(String email, String role) {
}
