package com.innowise.gateway.util;

import com.innowise.gateway.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  @Mock
  private JwtConfig jwtConfig;

  @Mock
  private JwtDecoder jwtDecoder;

  @InjectMocks
  private JwtTokenProvider jwtTokenProvider;

  private final String validToken = "valid.token";

  private Jwt createValidJwt(String token, String tokenType) {
    return Jwt.withTokenValue(token)
            .header("alg", "HS256")
            .subject("123")
            .claim("role", "USER")
            .claim("token_type", tokenType)
            .build();
  }

  @Test
  void validateToken_shouldReturnTrueForValidToken() {
    Jwt validJwt = createValidJwt(validToken, "access");
    when(jwtDecoder.decode(validToken)).thenReturn(validJwt);
    assertThat(jwtTokenProvider.validateToken(validToken)).isTrue();
  }

  @Test
  void validateToken_shouldReturnFalseForInvalidToken() {
    when(jwtDecoder.decode("invalid")).thenThrow(new JwtException("Invalid"));
    assertThat(jwtTokenProvider.validateToken("invalid")).isFalse();
  }

  @Test
  void getUserIdFromToken_shouldReturnCorrectId() {
    Jwt jwt = createValidJwt(validToken, "access");
    when(jwtDecoder.decode(validToken)).thenReturn(jwt);
    assertThat(jwtTokenProvider.getUserIdFromToken(validToken)).isEqualTo(123L);
  }

  @Test
  void getRoleFromToken_shouldReturnCorrectRole() {
    Jwt jwt = createValidJwt(validToken, "access");
    when(jwtDecoder.decode(validToken)).thenReturn(jwt);
    assertThat(jwtTokenProvider.getRoleFromToken(validToken)).isEqualTo("USER");
  }

  @Test
  void isAccessToken_shouldReturnTrueForAccessToken() {
    Jwt jwt = createValidJwt(validToken, "access");
    when(jwtDecoder.decode(validToken)).thenReturn(jwt);
    assertThat(jwtTokenProvider.isAccessToken(validToken)).isTrue();
  }

  @Test
  void isAccessToken_shouldReturnFalseForRefreshToken() {
    Jwt jwt = createValidJwt("refresh.token", "refresh");
    when(jwtDecoder.decode("refresh.token")).thenReturn(jwt);
    assertThat(jwtTokenProvider.isAccessToken("refresh.token")).isFalse();
  }

  @Test
  void isRefreshToken_shouldReturnTrueForRefreshToken() {
    Jwt jwt = createValidJwt("refresh.token", "refresh");
    when(jwtDecoder.decode("refresh.token")).thenReturn(jwt);
    assertThat(jwtTokenProvider.isRefreshToken("refresh.token")).isTrue();
  }
}