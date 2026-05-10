package com.innowise.gateway.service;

import com.innowise.gateway.client.AuthClient;
import com.innowise.gateway.client.UserClient;
import com.innowise.gateway.dto.*;
import com.innowise.gateway.service.impl.RegistrationOrchestratorImpl;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationOrchestratorImplTest {

  @Mock
  private UserClient userClient;

  @Mock
  private AuthClient authClient;

  private CircuitBreaker userCircuitBreaker;
  private CircuitBreaker authCircuitBreaker;
  private Retry rollbackRetry;

  @InjectMocks
  private RegistrationOrchestratorImpl orchestrator;

  private final RegistrationRequest request = new RegistrationRequest(
          "john_doe", "password123", "John", "Doe", LocalDate.of(1990, 1, 1), "john@example.com"
  );

  @BeforeEach
  void setUp() {
    CircuitBreakerConfig disabledConfig = CircuitBreakerConfig.custom()
            .enableAutomaticTransitionFromOpenToHalfOpen()
            .slidingWindowSize(100)
            .failureRateThreshold(100)
            .build();
    CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(disabledConfig);
    userCircuitBreaker = cbRegistry.circuitBreaker("user-service");
    authCircuitBreaker = cbRegistry.circuitBreaker("auth-service");

    RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(1)
            .waitDuration(Duration.ZERO)
            .build();
    RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
    rollbackRetry = retryRegistry.retry("user-service-rollback");

    orchestrator = new RegistrationOrchestratorImpl(userClient, authClient,
            authCircuitBreaker, userCircuitBreaker, rollbackRetry);
  }

  @Test
  void register_shouldSucceedAndReturnResponse() {
    UserShortDto userDto = new UserShortDto(1L);
    when(userClient.createUser(any(UserCreateDto.class))).thenReturn(Mono.just(userDto));
    when(authClient.register(any(CredentialsRequest.class))).thenReturn(Mono.empty());

    StepVerifier.create(orchestrator.register(request))
            .expectNextMatches(resp -> resp.userId() == 1L && resp.message().contains("successfully"))
            .verifyComplete();

    verify(userClient).createUser(any(UserCreateDto.class));
    verify(authClient).register(any(CredentialsRequest.class));
    verify(userClient, never()).rollbackUser(anyLong());
  }

  @Test
  void register_whenAuthFails_shouldRollbackAndReturnError() {
    UserShortDto userDto = new UserShortDto(1L);
    when(userClient.createUser(any(UserCreateDto.class))).thenReturn(Mono.just(userDto));
    when(authClient.register(any(CredentialsRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Auth service unavailable")));
    when(userClient.rollbackUser(1L)).thenReturn(Mono.empty());

    StepVerifier.create(orchestrator.register(request))
            .expectErrorSatisfies(throwable -> {
              assertThat(throwable)
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("Registration failed. User soft-deleted.");
            })
            .verify();

    verify(userClient).rollbackUser(1L);
  }

  @Test
  void register_whenUserCreationFails_shouldNotCallAuthAndPropagateError() {
    when(userClient.createUser(any(UserCreateDto.class)))
            .thenReturn(Mono.error(new RuntimeException("User service down")));

    StepVerifier.create(orchestrator.register(request))
            .expectError(RuntimeException.class)
            .verify();

    verify(authClient, never()).register(any());
    verify(userClient, never()).rollbackUser(anyLong());
  }
}