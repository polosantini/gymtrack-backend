package com.gymtrack.pagos.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagoTest {

    @Test
    @DisplayName("Un pago nuevo nace REGISTRADO y con fecha tras @PrePersist")
    void pagoNuevoNaceRegistrado() {

        Pago pago = new Pago();

        assertNull(pago.getEstado());

        assertNull(pago.getFecha());

        pago.defaultCampos();

        assertEquals(EstadoPago.REGISTRADO, pago.getEstado());

        assertNotNull(pago.getFecha());
    }
}