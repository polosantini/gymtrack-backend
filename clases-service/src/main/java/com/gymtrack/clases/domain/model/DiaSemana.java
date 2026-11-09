package com.gymtrack.clases.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Días de la semana en los que se dicta una clase.
 * El orden del enum (LUNES..DOMINGO) coincide con {@link DayOfWeek}.
 */
public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    /** Día de la semana que corresponde a una fecha concreta. */
    public static DiaSemana de(LocalDate fecha) {
        return de(fecha.getDayOfWeek());
    }

    public static DiaSemana de(DayOfWeek dayOfWeek) {
        return values()[dayOfWeek.getValue() - 1];
    }

    /**
     * Convierte un texto a DiaSemana sin importar mayúsculas ni tildes
     * ("lunes", "Miércoles", "SABADO"...). Lo usa Jackson al leer el JSON.
     */
    @JsonCreator
    public static DiaSemana desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
        try {
            return valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Día inválido: " + texto + ". Valores permitidos: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO");
        }
    }
}
