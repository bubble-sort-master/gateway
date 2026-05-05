package com.innowise.gateway.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableWireMock
@AutoConfigureWebTestClient
class GatewayIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  void resetWireMock() {
    WireMock.reset();
  }

  @Test
  void publicLoginEndpoint_shouldBeAccessibleWithoutToken() {
    stubFor(post("/auth/token")
            .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"token\": \"fake-jwt\"}")));

    webTestClient.post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"username\":\"user\",\"password\":\"pass\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.token").isEqualTo("fake-jwt");
  }

  @Test
  void protectedUserEndpoint_withoutToken_shouldReturn401() {
    webTestClient.get()
            .uri("/api/users/123")
            .exchange()
            .expectStatus().isUnauthorized();
  }

  @Test
  void register_shouldOrchestrateSuccessfully() {
    stubFor(post(urlEqualTo("/api/users"))
            .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"id\":1,\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"john@example.com\"}")));

    stubFor(post(urlEqualTo("/auth/register"))
            .willReturn(aResponse().withStatus(200)));

    String requestBody = """
                {
                  "username": "john_doe",
                  "password": "secret",
                  "name": "John",
                  "surname": "Doe",
                  "birthDate": "1990-01-01",
                  "email": "john@example.com"
                }
                """;

    webTestClient.post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.userId").isEqualTo(1)
            .jsonPath("$.message").isEqualTo("User registered successfully");
  }

  @Test
  void register_authFails_shouldRollbackUser() {
    stubFor(post(urlEqualTo("/api/users"))
            .willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody("{\"id\":1}")));

    stubFor(post(urlEqualTo("/auth/register"))
            .willReturn(aResponse().withStatus(500)));

    stubFor(delete(urlEqualTo("/api/users/1"))
            .willReturn(aResponse().withStatus(204)));

    String requestBody = """
                {
                  "username": "john_doe",
                  "password": "secret",
                  "name": "John",
                  "surname": "Doe",
                  "birthDate": "1990-01-01",
                  "email": "john@example.com"
                }
                """;

    webTestClient.post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().is5xxServerError();

    verify(deleteRequestedFor(urlEqualTo("/api/users/1")));
  }

  @Test
  void circuitBreaker_shouldOpenAfterFailures() {
    stubFor(post(urlEqualTo("/auth/register")).willReturn(aResponse().withStatus(500)));

    String requestBody = """
                {
                  "username": "john_doe",
                  "password": "secret",
                  "name": "John",
                  "surname": "Doe",
                  "birthDate": "1990-01-01",
                  "email": "john@example.com"
                }
                """;

    for (int i = 0; i < 5; i++) {
      webTestClient.post()
              .uri("/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(requestBody)
              .exchange()
              .expectStatus().is5xxServerError();
    }

    stubFor(post(urlEqualTo("/auth/register")).willReturn(aResponse().withStatus(200)));

    webTestClient.post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().is5xxServerError();
  }
}