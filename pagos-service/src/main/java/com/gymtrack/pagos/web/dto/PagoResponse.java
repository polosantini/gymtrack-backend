package com.gymtrack.pagos.web.dto;

import com.gymtrack.pagos.domain.model.EstadoPago;
import com.gymtrack.pagos.domain.model.PeriodoPago;
import com.gymtrack.pagos.domain.model.PlanPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse (
        Long id,
        Long miembroId,
        String plan,
        String periodo,
        BigDecimal monto,
        String metodo,
        String estado,
        LocalDateTime fecha
){}
