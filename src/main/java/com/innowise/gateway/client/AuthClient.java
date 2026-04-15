package com.innowise.gateway.client;

import com.innowise.gateway.config.UriConfig;
import com.innowise.gateway.dto.CredentialsRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthClient {

  private final WebClient webClient;

  public AuthClient(WebClient.Builder builder, UriConfig uriConfig) {
    this.webClient = builder.baseUrl(uriConfig.getAuthServiceUrl()).build();
  }

  public Mono<Void> register(CredentialsRequest request) {
    return webClient.post()
            .uri("/auth/register")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class);
  }
}