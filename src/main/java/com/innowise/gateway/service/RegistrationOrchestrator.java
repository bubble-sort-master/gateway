package com.innowise.gateway.service;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.dto.RegistrationResponse;
import reactor.core.publisher.Mono;

/**
 * Orchestrates user registration across User and Auth services.
 */
public interface RegistrationOrchestrator {

  /**
   * Creates user profile and registers credentials.
   * <p>If credential registration fails, created user is soft-deleted.
   *
   * @param request registration data
   * @return response with generated user ID
   */
  Mono<RegistrationResponse> register(RegistrationRequest request);
}