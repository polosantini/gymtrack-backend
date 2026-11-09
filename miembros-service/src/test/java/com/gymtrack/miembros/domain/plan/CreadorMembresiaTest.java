package com.gymtrack.miembros.domain.plan;

import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.gymtrack.miembros.domain.model.TipoPlanMembresia.*;
import static org.junit.jupiter.api.Assertions.*;

class CreadorMembresiaTest {
    private final CreadorMembresia creador = new CreadorMembresia();

    @Test
    @DisplayName("Verificar que la fábrica devuelva la implementación correcta")
    void implementacionCorrecta() {
        PlanMembresia planBasico = creador.crear(BASICO);
        PlanMembresia planFull = creador.crear(TipoPlanMembresia.FULL);
        PlanMembresia planPremium = creador.crear(PREMIUM);

        assertInstanceOf(PlanBasico.class, planBasico);
        assertInstanceOf(PlanFull.class, planFull);
        assertInstanceOf(PlanPremium.class, planPremium);
    }

    @Test
    @DisplayName("Las 3 membresías permiten acceso con activa")
    void permitirAccesoActiva() {
        for (TipoPlanMembresia tipoPlan : TipoPlanMembresia.values()) {
            assertTrue(creador.crear(tipoPlan).puedeAcceder(EstadoMembresia.ACTIVA));
        }
    }

    @Test
    @DisplayName("Las 3 membresías no permiten acceso con suspendida")
    void denegarAccesoSuspendida() {
        for (TipoPlanMembresia tipoPlan : TipoPlanMembresia.values()) {
            assertFalse(creador.crear(tipoPlan).puedeAcceder(EstadoMembresia.SUSPENDIDA));
        }
    }

    @Test
    @DisplayName("Las 3 membresías no permiten acceso con vencida")
    void denegarAccesoVencida() {
        for (TipoPlanMembresia tipoPlan : TipoPlanMembresia.values()) {
            assertFalse(creador.crear(tipoPlan).puedeAcceder(EstadoMembresia.VENCIDA));
        }
    }

    @Test
    @DisplayName("Verificar que getNombre() devuelva el string correcto")
    void getStringCorrecto() {
        assertEquals("BASICO", creador.crear(BASICO).getNombre());
        assertEquals("FULL", creador.crear(FULL).getNombre());
        assertEquals("PREMIUM", creador.crear(PREMIUM).getNombre());
    }
}
