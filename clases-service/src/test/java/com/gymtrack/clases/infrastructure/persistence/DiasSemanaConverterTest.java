package com.gymtrack.clases.infrastructure.persistence;

import com.gymtrack.clases.domain.model.DiaSemana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiasSemanaConverterTest {

    private final DiasSemanaConverter converter = new DiasSemanaConverter();

    @Test
    @DisplayName("Guarda los días ordenados de lunes a domingo separados por coma")
    void convierteAColumnaOrdenada() {
        String columna = converter.convertToDatabaseColumn(Set.of(DiaSemana.VIERNES, DiaSemana.LUNES));

        assertEquals("LUNES,VIERNES", columna);
    }

    @Test
    @DisplayName("Lee la columna de vuelta como Set de días")
    void leeColumnaComoSet() {
        Set<DiaSemana> dias = converter.convertToEntityAttribute("MARTES,JUEVES");

        assertEquals(EnumSet.of(DiaSemana.MARTES, DiaSemana.JUEVES), dias);
    }

    @Test
    @DisplayName("Columna vacía o null se lee como Set vacío")
    void columnaVaciaEsSetVacio() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
    }
}
