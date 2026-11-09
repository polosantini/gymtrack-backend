package com.gymtrack.clases.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaTest {

    private Reserva reservaCon(EstadoReserva estado, Integer posicionEspera) {
        Reserva reserva = new Reserva();
        reserva.setEstado(estado);
        reserva.setPosicionEspera(posicionEspera);
        return reserva;
    }

    @Test
    @DisplayName("EN_ESPERA sin posición de espera no es válida")
    void enEsperaSinPosicionFalla() {
        assertThrows(IllegalStateException.class,
                () -> reservaCon(EstadoReserva.EN_ESPERA, null).validarPosicionEspera());
    }

    @Test
    @DisplayName("EN_ESPERA con posición 1 es válida")
    void enEsperaConPosicionEsValida() {
        assertDoesNotThrow(() -> reservaCon(EstadoReserva.EN_ESPERA, 1).validarPosicionEspera());
    }

    @Test
    @DisplayName("CONFIRMADA y CANCELADA aceptan posición de espera null")
    void confirmadaYCanceladaAceptanNull() {
        assertDoesNotThrow(() -> reservaCon(EstadoReserva.CONFIRMADA, null).validarPosicionEspera());
        assertDoesNotThrow(() -> reservaCon(EstadoReserva.CANCELADA, null).validarPosicionEspera());
    }
}
