package com.innowise.gateway.util;

import com.innowise.gateway.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;
  private final JwtDecoder jwtDecoder;

  public boolean validateToken(String token) {
    try {
      jwtDecoder.decode(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public Long getUserIdFromToken(String token) {
    Jwt jwt = jwtDecoder.decode(token);
    return Long.valueOf(jwt.getSubject());
  }

  public String getRoleFromToken(String token) {
    Jwt jwt = jwtDecoder.decode(token);
    return jwt.getClaimAsString("role");
  }

  public boolean isAccessToken(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      return "access".equals(jwt.getClaimAsString("token_type"));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);
      return "refresh".equals(jwt.getClaimAsString("token_type"));
    } catch (Exception e) {
      return false;
    }
  }
}