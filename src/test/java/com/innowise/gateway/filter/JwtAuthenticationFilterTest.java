package com.innowise.gateway.filter;

import com.innowise.gateway.util.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private WebFilterChain filterChain;

  @InjectMocks
  private JwtAuthenticationFilter filter;

  @Test
  void filter_shouldPermitPublicEndpointWithoutToken() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/auth/login")
    );
    when(filterChain.filter(any())).thenReturn(Mono.empty());

    filter.filter(exchange, filterChain).block();

    verify(filterChain).filter(exchange);
    verifyNoInteractions(jwtTokenProvider);
  }

  @Test
  void filter_shouldReturnUnauthorizedWhenNoAuthHeader() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/123")
    );

    filter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(filterChain, never()).filter(any());
  }

  @Test
  void filter_shouldReturnUnauthorizedWhenInvalidToken() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/123")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
    );
    when(jwtTokenProvider.validateToken("invalid.token")).thenReturn(false);

    filter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(filterChain, never()).filter(any());
  }

  @Test
  void filter_shouldAddHeadersAndForwardWhenValidAccessToken() {
    String token = "valid.access.token";
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/123")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
    );

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.isAccessToken(token)).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(123L);
    when(jwtTokenProvider.getRoleFromToken(token)).thenReturn("USER");

    ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
    when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

    filter.filter(exchange, filterChain).block();

    ServerWebExchange mutatedExchange = exchangeCaptor.getValue();
    assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("123");
    assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
  }

  @Test
  void filter_shouldReturnUnauthorizedWhenRefreshTokenUsedForProtectedEndpoint() {
    String token = "valid.refresh.token";
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/users/123")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
    );

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.isAccessToken(token)).thenReturn(false);

    filter.filter(exchange, filterChain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(filterChain, never()).filter(any());
  }
}