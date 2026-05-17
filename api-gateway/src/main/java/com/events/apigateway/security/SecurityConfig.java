package com.events.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter) {

		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.logout(ServerHttpSecurity.LogoutSpec::disable)
				.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint((exchange, exception) -> {
							exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
							return Mono.empty();
						})
						.accessDeniedHandler((exchange, exception) -> {
							exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
							return Mono.empty();
						}))
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/health").permitAll()
						.pathMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
						.pathMatchers("/events/**").authenticated()
						.pathMatchers("/reservations/**").authenticated()
						.anyExchange().authenticated())
				.addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.build();
	}
}
