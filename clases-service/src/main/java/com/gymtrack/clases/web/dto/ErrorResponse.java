package com.gymtrack.clases.web.dto;

import java.time.LocalDateTime;

/** Formato uniforme de error, igual al de miembros-service y pagos-service. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
