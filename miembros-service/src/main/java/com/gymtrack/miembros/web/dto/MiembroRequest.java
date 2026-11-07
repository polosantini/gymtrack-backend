package com.gymtrack.miembros.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MiembroRequest(
        @NotBlank @Size(max=255) String nombre,
        @NotBlank @Email(message = "El email no tiene un formato válido") @Size(max = 255) String email
        ) {}
