package com.innowise.gateway.config;

import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class JwtDecoderConfig {

  private final JwtConfig jwtConfig;

  @Bean
  public JwtDecoder jwtDecoder() {
    SecretKeySpec secretKey = new SecretKeySpec(
            jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
    );
    return NimbusJwtDecoder.withSecretKey(secretKey).build();
  }
}