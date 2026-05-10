package com.innowise.gateway.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 2, max = 100) String surname,
        @Past LocalDate birthDate,
        @NotBlank @Email String email
) {}