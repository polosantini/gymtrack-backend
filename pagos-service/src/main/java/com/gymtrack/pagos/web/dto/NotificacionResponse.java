package com.gymtrack.pagos.web.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        Long miembroId,
        String tipo,
        String mensaje,
        LocalDateTime fecha,
        boolean leida
) {}
