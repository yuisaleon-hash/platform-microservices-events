package com.events.eventservice.security;

import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            validateExpiration(claims);
            return true;
        } catch (RuntimeException exception) {
            System.out.println("ERROR JWT: " + exception.getClass().getName());
            System.out.println("MENSAJE JWT: " + exception.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        String subject = extractAllClaims(token).getSubject();

        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("JWT subject is missing");
        }

        return subject;
    }

    public String extractRole(String token) {
        String role = extractAllClaims(token).get("role", String.class);

        if (role == null || role.isBlank()) {
            throw new BadCredentialsException("JWT role is missing");
        }

        return role;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void validateExpiration(Claims claims) {
        Date expiration = claims.getExpiration();

        if (expiration != null && expiration.before(new Date())) {
            throw new BadCredentialsException("JWT expired");
        }
    }
}
