package com.events.apigateway.security;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (isPublicRoute(exchange)) {
			return chain.filter(exchange);
		}

		String authorizationHeader = exchange.getRequest()
				.getHeaders()
				.getFirst(HttpHeaders.AUTHORIZATION);

		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return unauthorized(exchange);
		}

		try {
			String token = authorizationHeader.substring(BEARER_PREFIX.length());
			JwtPrincipal principal = jwtService.validateToken(token);
			UsernamePasswordAuthenticationToken authentication = buildAuthentication(principal, token);

			return chain.filter(exchange)
					.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
		} catch (RuntimeException exception) {
			return unauthorized(exchange);
		}
	}

	private boolean isPublicRoute(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		HttpMethod method = exchange.getRequest().getMethod();
		return HttpMethod.POST.equals(method)
				&& ("/auth/login".equals(path) || "/auth/register".equals(path));
	}

	private UsernamePasswordAuthenticationToken buildAuthentication(JwtPrincipal principal, String token) {
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + principal.role());
		return UsernamePasswordAuthenticationToken.authenticated(
				principal.email(),
				token,
				List.of(authority));
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}
}
