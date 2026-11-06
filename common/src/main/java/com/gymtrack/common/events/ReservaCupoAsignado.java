package com.gymtrack.common.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Evento publicado cuando a un miembro (por ejemplo, de la lista de espera)
 * se le asigna un cupo en una clase.
 *
 * <p>Routing key: {@link EventosConfig#RK_RESERVA_CUPO_ASIGNADO}.</p>
 *
 * @param miembroId   identificador del miembro que recibe el cupo
 * @param claseNombre nombre de la clase (para la notificación)
 * @param fecha       fecha de la clase
 * @param hora        hora de inicio de la clase
 */
@JsonPropertyOrder({"miembroId", "claseNombre", "fecha", "hora"})
public record ReservaCupoAsignado(
        Long miembroId,
        String claseNombre,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fecha,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime hora
) {
}
