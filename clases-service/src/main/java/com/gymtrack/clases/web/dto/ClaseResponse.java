package com.gymtrack.clases.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymtrack.clases.domain.model.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Clase del catálogo con los cupos libres de una sesión concreta.
 *
 * @param fecha       fecha de la sesión a la que corresponden los cupos
 * @param cuposLibres aforo menos las reservas CONFIRMADA de esa (clase, fecha)
 */
public record ClaseResponse(
        Long id,
        String nombre,
        String instructor,
        Set<DiaSemana> dias,
        @JsonFormat(pattern = "HH:mm") LocalTime hora,
        Integer duracionMinutos,
        Integer aforo,
        String sala,
        LocalDate fecha,
        Integer cuposLibres
) {}
