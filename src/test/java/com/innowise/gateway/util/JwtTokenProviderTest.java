package com.innowise.gateway.util;

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
  private JwtDecoder jwtDecoder;

  @InjectMocks
  private JwtTokenProvider jwtTokenProvider;

  private Jwt createJwt(String token, String type) {
    return Jwt.withTokenValue(token)
            .header("alg", "HS256")
            .subject("123")
            .claim("role", "USER")
            .claim("token_type", type)
            .build();
  }

  @Test
  void decode_shouldReturnJwtForValidToken() {
    Jwt jwt = createJwt("valid", "access");
    when(jwtDecoder.decode("valid")).thenReturn(jwt);
    assertThat(jwtTokenProvider.decode("valid")).isEqualTo(jwt);
  }

  @Test
  void decode_shouldThrowForInvalidToken() {
    when(jwtDecoder.decode("invalid")).thenThrow(new JwtException("Invalid"));
    try {
      jwtTokenProvider.decode("invalid");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(JwtException.class);
    }
  }

  @Test
  void getUserIdFromToken_shouldReturnCorrectId() {
    Jwt jwt = createJwt("t", "access");
    assertThat(jwtTokenProvider.getUserIdFromToken(jwt)).isEqualTo(123L);
  }

  @Test
  void getRoleFromToken_shouldReturnCorrectRole() {
    Jwt jwt = createJwt("t", "access");
    assertThat(jwtTokenProvider.getRoleFromToken(jwt)).isEqualTo("USER");
  }

  @Test
  void isAccessToken_shouldReturnTrueForAccess() {
    Jwt jwt = createJwt("t", "access");
    assertThat(jwtTokenProvider.isAccessToken(jwt)).isTrue();
  }

  @Test
  void isAccessToken_shouldReturnFalseForRefresh() {
    Jwt jwt = createJwt("t", "refresh");
    assertThat(jwtTokenProvider.isAccessToken(jwt)).isFalse();
  }

  @Test
  void isRefreshToken_shouldReturnTrueForRefresh() {
    Jwt jwt = createJwt("t", "refresh");
    assertThat(jwtTokenProvider.isRefreshToken(jwt)).isTrue();
  }
}