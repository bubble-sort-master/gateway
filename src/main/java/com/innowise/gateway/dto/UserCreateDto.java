package com.innowise.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserCreateDto(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 2, max = 100) String surname,
        @Past LocalDate birthDate,
        @NotBlank @Email String email
) {}