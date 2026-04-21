package com.innowise.gateway.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtDecoder jwtDecoder;

  public Jwt decode(String token) {
    return jwtDecoder.decode(token);
  }

  public Long getUserIdFromToken(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }

  public String getRoleFromToken(Jwt jwt) {
    return jwt.getClaimAsString("role");
  }

  public boolean isAccessToken(Jwt jwt) {
    return "access".equals(jwt.getClaimAsString("token_type"));
  }

  public boolean isRefreshToken(Jwt jwt) {
    return "refresh".equals(jwt.getClaimAsString("token_type"));
  }
}