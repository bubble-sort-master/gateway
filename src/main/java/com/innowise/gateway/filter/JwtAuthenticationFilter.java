package com.innowise.gateway.filter;

import com.innowise.gateway.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

  private final JwtTokenProvider jwtTokenProvider;

  private static final List<String> PERMIT_ALL = List.of(
          "/auth/login", "/auth/register"
  );

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    if (PERMIT_ALL.stream().anyMatch(path::startsWith)) {
      return chain.filter(exchange);
    }

    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return unauthorized(exchange.getResponse());
    }

    String token = authHeader.substring(7).trim();

    Jwt jwt;
    try {
      jwt = jwtTokenProvider.decode(token);
    } catch (Exception e) {
      return unauthorized(exchange.getResponse());
    }

    if (!jwtTokenProvider.isAccessToken(jwt)) {
      return unauthorized(exchange.getResponse());
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
    String role = jwtTokenProvider.getRoleFromToken(jwt);

    ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", userId.toString())
            .header("X-User-Role", role)
            .build();

    return chain.filter(exchange.mutate().request(mutated).build());
  }

  private Mono<Void> unauthorized(ServerHttpResponse response) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    return response.setComplete();
  }
}