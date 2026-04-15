package com.innowise.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

  @Bean
  public CircuitBreaker authCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("auth-service");
  }

  @Bean
  public CircuitBreaker userCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("user-service");
  }

  @Bean
  public Retry rollbackRetry(RetryRegistry registry) {
    return registry.retry("user-service-rollback");
  }
}