package com.gymtrack.clases.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservaRequest(
        @NotNull(message = "El miembroId es obligatorio") Long miembroId,
        @NotNull(message = "El claseId es obligatorio") Long claseId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha
) {}
