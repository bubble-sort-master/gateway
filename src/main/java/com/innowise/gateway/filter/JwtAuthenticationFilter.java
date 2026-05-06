package com.innowise.gateway.filter;

import com.innowise.gateway.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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
          "/auth/token", "/auth/register", "/auth/refresh", "/auth/validate", "/actuator"
  );

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    log.info("Filter invoked for path: {}", path);

    if (PERMIT_ALL.stream().anyMatch(path::startsWith)) {
      return chain.filter(exchange);
    }

    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    log.info("Authorization header: {}", authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("Missing or malformed authorization header");
      return unauthorized(exchange.getResponse());
    }

    String token = authHeader.substring(7).trim();
    log.debug("Token: {}", token);

    Jwt jwt;
    try {
      jwt = jwtTokenProvider.decode(token);
      log.debug("JWT decoded successfully, subject: {}", jwt.getSubject());
    } catch (Exception e) {
      log.error("JWT decode failed", e);
      return unauthorized(exchange.getResponse());
    }

    if (!jwtTokenProvider.isAccessToken(jwt)) {
      log.warn("Token is not an access token");
      return unauthorized(exchange.getResponse());
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
    String role = jwtTokenProvider.getRoleFromToken(jwt);
    log.debug("Extracted userId: {}, role: {}", userId, role);

    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);

    ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", userId.toString())
            .header("X-User-Role", role)
            .build();

    return chain.filter(exchange.mutate().request(mutated).build())
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
  }

  private Mono<Void> unauthorized(ServerHttpResponse response) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    return response.setComplete();
  }
}