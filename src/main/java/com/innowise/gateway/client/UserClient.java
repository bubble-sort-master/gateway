package com.innowise.gateway.client;

import com.innowise.gateway.dto.UserCreateDto;
import com.innowise.gateway.dto.UserShortDto;
import com.innowise.gateway.config.UriConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserClient {

  private final WebClient webClient;

  @Value("${internal.secret}")
  private String internalSecret;

  public UserClient(WebClient.Builder builder, UriConfig uriConfig) {
    this.webClient = builder.baseUrl(uriConfig.getUserServiceUrl()).build();
  }

  public Mono<UserShortDto> createUser(UserCreateDto dto) {
    return webClient.post()
            .uri("/api/users")
            .bodyValue(dto)
            .retrieve()
            .bodyToMono(UserShortDto.class);
  }

  public Mono<Void> rollbackUser(Long userId) {
    return webClient.delete()
            .uri("/api/internal/users/{id}/rollback", userId)
            .header("X-Internal-Secret", internalSecret)
            .retrieve()
            .bodyToMono(Void.class);
  }
}