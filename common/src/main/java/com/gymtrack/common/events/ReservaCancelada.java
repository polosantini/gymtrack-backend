package com.gymtrack.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;

/**
 * Evento publicado cuando un miembro cancela una reserva de clase.
 *
 * <p>Routing key: {@link EventosConfig#RK_RESERVA_CANCELADA}.</p>
 *
 * @param reservaId  identificador de la reserva cancelada
 * @param claseId    identificador de la clase
 * @param fecha      fecha de la clase
 * @param liberaCupo {@code true} si la cancelación deja un cupo libre para otro miembro
 */
@JsonPropertyOrder({"reservaId", "claseId", "fecha", "liberaCupo"})
public record ReservaCancelada(
        Long reservaId,
        Long claseId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fecha,
        boolean liberaCupo
) {
}
