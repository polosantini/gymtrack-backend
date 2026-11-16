package com.gymtrack.miembros.application;

import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.plan.CreadorMembresia;
import com.gymtrack.miembros.domain.repository.MembresiaRepository;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import com.gymtrack.miembros.web.dto.AccesoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Contrato de GET /api/miembros/{id}/acceso.
 *
 * Cubre las 3 combinaciones de estado (SUSPENDIDA, ACTIVA, VENCIDA) y los
 * bordes de fecha: vigente, vencida ayer, vence hoy y fecha futura con
 * estado VENCIDA (prevalece el estado).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiembroServiceImpl.verificarAcceso — /acceso")
class MiembroServiceImplAccesoTest {

    @Mock
    private MiembroRepository miembroRepository;

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private CreadorMembresia creadorMembresia;

    @InjectMocks
    private MiembroServiceImpl service;

    private static final Long MIEMBRO_ID = 1L;

    // ------------------------------------------------------------------
    // Helpers para no repetir la construcción de entidades
    // ------------------------------------------------------------------

    private Miembro miembroCon(Membresia membresia) {
        Miembro m = new Miembro();
        m.setNombre("Tester");
        m.setEmail("tester@example.com");
        m.setMembresia(membresia);
        return m;
    }

    private Membresia membresia(EstadoMembresia estado,
                                TipoPlanMembresia plan,
                                LocalDate inicio,
                                LocalDate vencimiento) {
        Membresia mem = new Membresia();
        mem.setEstado(estado);
        mem.setPlan(plan);
        mem.setFechaInicio(inicio);
        mem.setFechaVencimiento(vencimiento);
        return mem;
    }

    // ------------------------------------------------------------------
    // Casos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sin membresía: inactivo y plan nulo")
    void sinMembresia_devuelveInactivoYPlanNulo() {
        Miembro m = miembroCon(null);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isFalse();
        assertThat(r.plan()).isNull();
    }

    @Test
    @DisplayName("SUSPENDIDA con fecha futura: inactivo, conserva el plan")
    void suspendidaVigente_devuelveInactivoConPlan() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroCon(membresia(
                EstadoMembresia.SUSPENDIDA,
                TipoPlanMembresia.FULL,
                hoy.minusDays(5),
                hoy.plusDays(10)
        ));
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isFalse();
        assertThat(r.plan()).isEqualTo("FULL");
    }

    @Test
    @DisplayName("ACTIVA vigente: activo")
    void activaVigente_devuelveActivo() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroCon(membresia(
                EstadoMembresia.ACTIVA,
                TipoPlanMembresia.FULL,
                hoy.minusDays(10),
                hoy.plusDays(10)
        ));
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isTrue();
        assertThat(r.plan()).isEqualTo("FULL");
    }

    @Test
    @DisplayName("ACTIVA con fecha ya vencida: inactivo (endurecido)")
    void activaVencida_devuelveInactivo() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroCon(membresia(
                EstadoMembresia.ACTIVA,
                TipoPlanMembresia.FULL,
                hoy.minusDays(30),
                hoy.minusDays(1)
        ));
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isFalse();
        assertThat(r.plan()).isEqualTo("FULL");
    }

    @Test
    @DisplayName("ACTIVA que vence hoy: todavía activo (frontera)")
    void activaQueVenceHoy_devuelveActivo() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroCon(membresia(
                EstadoMembresia.ACTIVA,
                TipoPlanMembresia.PREMIUM,
                hoy.minusDays(30),
                hoy
        ));
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isTrue();
        assertThat(r.plan()).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("VENCIDA con fecha futura: inactivo (prevalece el estado)")
    void vencidaConFechaFutura_devuelveInactivo() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroCon(membresia(
                EstadoMembresia.VENCIDA,
                TipoPlanMembresia.BASICO,
                hoy.minusDays(60),
                hoy.plusDays(10)
        ));
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        AccesoResponse r = service.verificarAcceso(MIEMBRO_ID);

        assertThat(r.activa()).isFalse();
        assertThat(r.plan()).isEqualTo("BASICO");
    }
}