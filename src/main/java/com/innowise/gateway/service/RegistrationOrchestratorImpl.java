package com.innowise.gateway.service;

import com.innowise.gateway.client.AuthClient;
import com.innowise.gateway.client.UserClient;
import com.innowise.gateway.dto.CredentialsRequest;
import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.dto.RegistrationResponse;
import com.innowise.gateway.dto.UserCreateDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationOrchestratorImpl {

  private final UserClient userClient;
  private final AuthClient authClient;
  private final CircuitBreaker authCircuitBreaker;
  private final CircuitBreaker userCircuitBreaker;
  private final Retry rollbackRetry;

  public Mono<RegistrationResponse> register(RegistrationRequest request) {
    var userCreateDto = new UserCreateDto(
            request.name(), request.surname(), request.birthDate(), request.email());

    return userClient.createUser(userCreateDto)
            .transformDeferred(CircuitBreakerOperator.of(userCircuitBreaker))
            .flatMap(userShortDto -> {
              Long userId = userShortDto.id();

              var credentialsRequest = new CredentialsRequest(
                      request.username(), request.password(), userId);

              return authClient.register(credentialsRequest)
                      .transformDeferred(CircuitBreakerOperator.of(authCircuitBreaker))
                      .then(Mono.just(new RegistrationResponse(userId, "User registered successfully")))
                      .onErrorResume(e -> {
                        log.error("Auth failed. Rollback user {}", userId, e);
                        return userClient.deleteUser(userId)
                                .transformDeferred(RetryOperator.of(rollbackRetry))
                                .then(Mono.error(new RuntimeException("Registration failed. User soft-deleted.", e)));
                      });
            });
  }
}