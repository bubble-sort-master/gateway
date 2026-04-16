package com.innowise.gateway.controller;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.dto.RegistrationResponse;
import com.innowise.gateway.service.RegistrationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Handles user registration requests.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private final RegistrationOrchestrator orchestrator;

  /**
   * Registers a new user and creates corresponding credentials.
   *
   * @param request registration data (profile + credentials)
   * @return response with generated user ID
   */
  @PostMapping("/register")
  public Mono<ResponseEntity<RegistrationResponse>> register(@RequestBody RegistrationRequest request) {
    return orchestrator.register(request)
            .map(ResponseEntity::ok);
  }
}