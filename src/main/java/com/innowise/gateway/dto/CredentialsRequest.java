package com.innowise.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CredentialsRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Long userId
) {}