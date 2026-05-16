package com.events.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final String jwtSecret;

	public JwtService(@Value("${security.jwt.secret}") String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public JwtPrincipal validateToken(String token) {
		Claims claims = extractAllClaims(token);
		validateRequiredClaims(claims);
		validateExpiration(claims);
		return new JwtPrincipal(claims.getSubject(), claims.get("role", String.class));
	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (RuntimeException exception) {
			throw new BadCredentialsException("Invalid JWT", exception);
		}
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private void validateRequiredClaims(Claims claims) {
		String email = claims.getSubject();
		String role = claims.get("role", String.class);

		if (email == null || email.isBlank()) {
			throw new BadCredentialsException("JWT subject is missing");
		}

		if (role == null || role.isBlank()) {
			throw new BadCredentialsException("JWT role is missing");
		}
	}

	private void validateExpiration(Claims claims) {
		Date expiration = claims.getExpiration();
		if (expiration != null && expiration.before(new Date())) {
			throw new BadCredentialsException("JWT expired");
		}
	}
}
