package com.gymtrack.clases.web.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /api/reservas/{id}/asistencia: {"asistio": true} o {"asistio": false}. */
public record AsistenciaRequest(
        @NotNull(message = "El campo asistio es obligatorio") Boolean asistio
) {}
