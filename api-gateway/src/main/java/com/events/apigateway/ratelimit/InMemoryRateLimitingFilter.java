package com.events.apigateway.ratelimit;

import com.events.apigateway.security.JwtPrincipal;
import com.events.apigateway.security.JwtService;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InMemoryRateLimitingFilter implements WebFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";

	private final JwtService jwtService;
	private final RateLimitPolicy authPolicy;
	private final RateLimitPolicy apiPolicy;
	private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

	public InMemoryRateLimitingFilter(
			JwtService jwtService,
			@Value("${rate-limit.auth.capacity}") int authCapacity,
			@Value("${rate-limit.auth.refill-period-seconds}") long authRefillPeriodSeconds,
			@Value("${rate-limit.api.capacity}") int apiCapacity,
			@Value("${rate-limit.api.refill-period-seconds}") long apiRefillPeriodSeconds) {
		this.jwtService = jwtService;
		this.authPolicy = new RateLimitPolicy("auth", authCapacity, Duration.ofSeconds(authRefillPeriodSeconds));
		this.apiPolicy = new RateLimitPolicy("api", apiCapacity, Duration.ofSeconds(apiRefillPeriodSeconds));
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		RateLimitPolicy policy = resolvePolicy(exchange);

		if (policy == null) {
			return chain.filter(exchange);
		}

		String key = policy.name() + ":" + resolveClientKey(exchange);
		RateLimitBucket bucket = buckets.computeIfAbsent(key, ignored -> new RateLimitBucket(
				policy.capacity(),
				policy.refillPeriod()));

		if (!bucket.tryConsume()) {
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			return exchange.getResponse().setComplete();
		}

		return chain.filter(exchange);
	}

	private RateLimitPolicy resolvePolicy(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		HttpMethod method = exchange.getRequest().getMethod();

		if (HttpMethod.POST.equals(method)
				&& ("/auth/login".equals(path) || "/auth/register".equals(path))) {
			return authPolicy;
		}

		if (path.startsWith("/events/") || "/events".equals(path)
				|| path.startsWith("/reservations/") || "/reservations".equals(path)) {
			return apiPolicy;
		}

		return null;
	}

	private String resolveClientKey(ServerWebExchange exchange) {
		String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
			try {
				String token = authorizationHeader.substring(BEARER_PREFIX.length());
				JwtPrincipal principal = jwtService.validateToken(token);
				return "user:" + principal.email();
			} catch (RuntimeException exception) {
				return "ip:" + resolveClientIp(exchange);
			}
		}

		return "ip:" + resolveClientIp(exchange);
	}

	private String resolveClientIp(ServerWebExchange exchange) {
		String forwardedFor = exchange.getRequest().getHeaders().getFirst(X_FORWARDED_FOR);

		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}

		InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
		if (remoteAddress == null || remoteAddress.getAddress() == null) {
			return "unknown";
		}

		return remoteAddress.getAddress().getHostAddress();
	}

	private record RateLimitPolicy(String name, int capacity, Duration refillPeriod) {
	}

	private static final class RateLimitBucket {

		private final int capacity;
		private final Duration refillPeriod;
		private int tokens;
		private Instant lastRefill;

		private RateLimitBucket(int capacity, Duration refillPeriod) {
			this.capacity = capacity;
			this.refillPeriod = refillPeriod;
			this.tokens = capacity;
			this.lastRefill = Instant.now();
		}

		private synchronized boolean tryConsume() {
			refillIfNeeded();

			if (tokens <= 0) {
				return false;
			}

			tokens--;
			return true;
		}

		private void refillIfNeeded() {
			Instant now = Instant.now();

			if (!now.isBefore(lastRefill.plus(refillPeriod))) {
				tokens = capacity;
				lastRefill = now;
			}
		}
	}
}
