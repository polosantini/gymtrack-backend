package com.gymtrack.miembros.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MembresiaTest {

    @Test
    @DisplayName("Una membresía nueva nace SUSPENDIDA tras @PrePersist")
    void membresiaNuevaNaceSuspendida() {
        Membresia membresia = new Membresia();

        assertNull(membresia.getEstado());

        membresia.suspendidaEstado();

        assertEquals(EstadoMembresia.SUSPENDIDA, membresia.getEstado());
    }
}