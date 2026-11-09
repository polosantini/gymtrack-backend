package com.gymtrack.clases.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reservas de una sesión (clase, fecha), separadas en confirmados y lista de espera.
 *
 * @param enEspera ordenada por posicionEspera ascendente (1, 2, 3...)
 */
public record ReservasClaseResponse(
        Long claseId,
        String claseNombre,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime hora,
        Integer aforo,
        Integer cuposLibres,
        List<ReservaResponse> confirmados,
        List<ReservaResponse> enEspera
) {}
