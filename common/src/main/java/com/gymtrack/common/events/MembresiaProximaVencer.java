package com.gymtrack.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * Evento publicado cuando la membresía de un miembro está por vencer.
 *
 * <p>Routing key: {@link EventosConfig#RK_MEMBRESIA_PROXIMA_VENCER}.</p>
 *
 * @param miembroId        identificador del miembro
 * @param nombre           nombre del miembro (para la notificación)
 * @param fechaVencimiento fecha en la que vence la membresía
 * @param diasRestantes    días que faltan para el vencimiento
 */
@JsonPropertyOrder({"miembroId", "nombre", "fechaVencimiento", "diasRestantes"})
public record MembresiaProximaVencer(
        Long miembroId,
        String nombre,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaVencimiento,
        int diasRestantes
) {
}
