package com.innowise.gateway.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
  @NotBlank private String secret;
  @Min(1000) private long accessTokenExpiration = 900000;
  @Min(1000) private long refreshTokenExpiration = 604800000;
}