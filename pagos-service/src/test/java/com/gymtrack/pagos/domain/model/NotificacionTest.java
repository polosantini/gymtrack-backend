package com.gymtrack.pagos.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificacionTest {

    @Test
    @DisplayName("Una notificación nueva nace no leída y con fecha tras @PrePersist")
    void notificacionNuevaNaceNoLeida() {
        Notificacion n = new Notificacion();

        n.defaultCampos();

        assertFalse(n.getLeida());
        assertNotNull(n.getFecha());
    }
}
