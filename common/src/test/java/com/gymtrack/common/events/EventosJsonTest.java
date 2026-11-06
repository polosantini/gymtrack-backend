package com.gymtrack.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica que los eventos se serializan a un JSON estable (mismo orden de
 * campos y mismo formato de fechas) y que se pueden leer de vuelta.
 */
class EventosJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private void assertJsonIdaYVuelta(Object evento, String jsonEsperado) throws Exception {
        String json = mapper.writeValueAsString(evento);
        assertEquals(jsonEsperado, json);
        assertEquals(evento, mapper.readValue(json, evento.getClass()));
    }

    @Test
    void pagoRegistrado() throws Exception {
        var evento = new PagoRegistrado(10L, 5L, "MENSUAL", "2026-11", new BigDecimal("80000.00"), "APROBADO");
        assertJsonIdaYVuelta(evento,
                "{\"pagoId\":10,\"miembroId\":5,\"plan\":\"MENSUAL\",\"periodo\":\"2026-11\","
                        + "\"monto\":80000.00,\"estado\":\"APROBADO\"}");
    }

    @Test
    void membresiaProximaVencer() throws Exception {
        var evento = new MembresiaProximaVencer(5L, "Ana Pérez", LocalDate.of(2026, 11, 30), 3);
        assertJsonIdaYVuelta(evento,
                "{\"miembroId\":5,\"nombre\":\"Ana Pérez\",\"fechaVencimiento\":\"2026-11-30\",\"diasRestantes\":3}");
    }

    @Test
    void reservaCancelada() throws Exception {
        var evento = new ReservaCancelada(99L, 7L, LocalDate.of(2026, 11, 10), true);
        assertJsonIdaYVuelta(evento,
                "{\"reservaId\":99,\"claseId\":7,\"fecha\":\"2026-11-10\",\"liberaCupo\":true}");
    }

    @Test
    void reservaCupoAsignado() throws Exception {
        var evento = new ReservaCupoAsignado(5L, "Spinning", LocalDate.of(2026, 11, 10), LocalTime.of(18, 30));
        assertJsonIdaYVuelta(evento,
                "{\"miembroId\":5,\"claseNombre\":\"Spinning\",\"fecha\":\"2026-11-10\",\"hora\":\"18:30\"}");
    }

    @Test
    void toStringLegibleParaLogs() {
        var evento = new ReservaCancelada(99L, 7L, LocalDate.of(2026, 11, 10), false);
        assertEquals("ReservaCancelada[reservaId=99, claseId=7, fecha=2026-11-10, liberaCupo=false]",
                evento.toString());
    }
}
