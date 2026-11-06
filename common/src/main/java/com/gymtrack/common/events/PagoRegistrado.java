package com.gymtrack.common.events;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/**
 * Evento publicado cuando se registra un pago de membresía.
 *
 * <p>Routing key: {@link EventosConfig#RK_PAGO_REGISTRADO}.</p>
 *
 * @param pagoId    identificador del pago
 * @param miembroId identificador del miembro que paga
 * @param plan      plan pagado (por ejemplo "MENSUAL")
 * @param periodo   periodo que cubre el pago (por ejemplo "2026-11")
 * @param monto     valor pagado
 * @param estado    estado del pago (por ejemplo "APROBADO")
 */
@JsonPropertyOrder({"pagoId", "miembroId", "plan", "periodo", "monto", "estado"})
public record PagoRegistrado(
        Long pagoId,
        Long miembroId,
        String plan,
        String periodo,
        BigDecimal monto,
        String estado
) {
}
