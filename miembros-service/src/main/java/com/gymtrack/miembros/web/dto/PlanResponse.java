package com.gymtrack.miembros.web.dto;

import java.math.BigDecimal;

public record PlanResponse(
        String nombre,
        BigDecimal precioMensual,
        BigDecimal precioTrimestral,
        BigDecimal precioAnual
) {
}
