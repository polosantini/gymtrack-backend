package com.gymtrack.clases.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaseTest {

    // 2026-11-17 es martes
    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);

    private Clase claseMartesYJueves() {
        Clase clase = new Clase();
        clase.setDias(Set.of(DiaSemana.MARTES, DiaSemana.JUEVES));
        return clase;
    }

    @Test
    @DisplayName("seDictaEl es true solo en los días de la clase")
    void seDictaElSoloEnSusDias() {
        Clase clase = claseMartesYJueves();

        assertTrue(clase.seDictaEl(MARTES_17));
        assertTrue(clase.seDictaEl(MARTES_17.plusDays(2)));   // jueves
        assertFalse(clase.seDictaEl(MARTES_17.plusDays(1)));  // miércoles
    }

    @Test
    @DisplayName("proximaSesionDesde devuelve la misma fecha si ese día hay clase")
    void proximaSesionIncluyeElDiaDeHoy() {
        assertEquals(MARTES_17, claseMartesYJueves().proximaSesionDesde(MARTES_17));
    }

    @Test
    @DisplayName("proximaSesionDesde salta al siguiente día con clase")
    void proximaSesionSaltaAlSiguienteDia() {
        LocalDate miercoles = MARTES_17.plusDays(1);
        LocalDate jueves = MARTES_17.plusDays(2);

        assertEquals(jueves, claseMartesYJueves().proximaSesionDesde(miercoles));
    }

    @Test
    @DisplayName("DiaSemana.desdeTexto acepta minúsculas y tildes")
    void diaSemanaDesdeTexto() {
        assertEquals(DiaSemana.MIERCOLES, DiaSemana.desdeTexto("miércoles"));
        assertEquals(DiaSemana.SABADO, DiaSemana.desdeTexto(" Sábado "));
        assertThrows(IllegalArgumentException.class, () -> DiaSemana.desdeTexto("feriado"));
    }
}
