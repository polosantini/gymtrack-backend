package com.gymtrack.clases.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @param estado         CONFIRMADA, EN_ESPERA o CANCELADA
 * @param posicionEspera posición en la lista de espera (solo si estado es EN_ESPERA; si no, null)
 */
public record ReservaResponse(
        Long id,
        Long miembroId,
        Long claseId,
        String claseNombre,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime hora,
        String estado,
        Integer posicionEspera
) {}
