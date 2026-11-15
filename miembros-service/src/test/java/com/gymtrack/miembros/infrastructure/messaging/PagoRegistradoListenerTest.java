package com.gymtrack.miembros.infrastructure.messaging;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoRegistradoListener — activación, renovación, idempotencia")
class PagoRegistradoListenerTest {

    @Mock
    private MiembroRepository miembroRepository;

    @InjectMocks
    private PagoRegistradoListener listener;

    private static final Long MIEMBRO_ID = 1L;
    private static final Long PAGO_ID = 100L;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Miembro miembroConMembresia(EstadoMembresia estado,
                                        TipoPlanMembresia plan,
                                        LocalDate inicio,
                                        LocalDate vencimiento,
                                        Long ultimoPagoId) {
        Membresia mem = new Membresia();
        mem.setEstado(estado);
        mem.setPlan(plan);
        mem.setFechaInicio(inicio);
        mem.setFechaVencimiento(vencimiento);
        mem.setUltimoPagoId(ultimoPagoId);

        Miembro m = new Miembro();
        m.setNombre("Tester");
        m.setEmail("tester@example.com");
        m.setMembresia(mem);
        return m;
    }

    private PagoRegistrado evento(String plan, String periodo) {
        return new PagoRegistrado(PAGO_ID, MIEMBRO_ID, plan, periodo,
                new BigDecimal("50000.00"), "REGISTRADO");
    }

    // ------------------------------------------------------------------
    // Casos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SUSPENDIDA → ACTIVA desde hoy con el periodo del pago")
    void suspendidaSeActivaDesdeHoy() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroConMembresia(
                EstadoMembresia.SUSPENDIDA, TipoPlanMembresia.BASICO,
                hoy.minusDays(5), hoy.plusDays(25), null);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("FULL", "MENSUAL"));

        Membresia mem = m.getMembresia();
        assertThat(mem.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(mem.getPlan()).isEqualTo(TipoPlanMembresia.FULL);
        assertThat(mem.getFechaInicio()).isEqualTo(hoy);
        assertThat(mem.getFechaVencimiento()).isEqualTo(hoy.plusMonths(1));
        assertThat(mem.getUltimoPagoId()).isEqualTo(PAGO_ID);
        verify(miembroRepository).save(m);
    }

    @Test
    @DisplayName("ACTIVA vigente → renueva desde vencimiento actual, fechaInicio intacta")
    void activaVigenteRenuevaDesdeVencimiento() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioOriginal = hoy.minusDays(20);
        LocalDate vencimientoOriginal = hoy.plusDays(10);
        Miembro m = miembroConMembresia(
                EstadoMembresia.ACTIVA, TipoPlanMembresia.FULL,
                inicioOriginal, vencimientoOriginal, 99L);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("FULL", "TRIMESTRAL"));

        Membresia mem = m.getMembresia();
        assertThat(mem.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(mem.getPlan()).isEqualTo(TipoPlanMembresia.FULL);
        assertThat(mem.getFechaInicio()).isEqualTo(inicioOriginal); // intacta
        assertThat(mem.getFechaVencimiento()).isEqualTo(vencimientoOriginal.plusMonths(3));
        assertThat(mem.getUltimoPagoId()).isEqualTo(PAGO_ID);
        verify(miembroRepository).save(m);
    }

    @Test
    @DisplayName("ACTIVA pero vencida → reinicia desde hoy (no extiende)")
    void activaVencidaReiniciaDesdeHoy() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroConMembresia(
                EstadoMembresia.ACTIVA, TipoPlanMembresia.FULL,
                hoy.minusDays(120), hoy.minusDays(30), 99L);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("FULL", "MENSUAL"));

        Membresia mem = m.getMembresia();
        assertThat(mem.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(mem.getFechaInicio()).isEqualTo(hoy);
        assertThat(mem.getFechaVencimiento()).isEqualTo(hoy.plusMonths(1));
        assertThat(mem.getUltimoPagoId()).isEqualTo(PAGO_ID);
    }

    @Test
    @DisplayName("VENCIDA → activa desde hoy con el nuevo plan")
    void vencidaSeActivaDesdeHoy() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroConMembresia(
                EstadoMembresia.VENCIDA, TipoPlanMembresia.BASICO,
                hoy.minusDays(120), hoy.minusDays(90), 50L);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("PREMIUM", "ANUAL"));

        Membresia mem = m.getMembresia();
        assertThat(mem.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(mem.getPlan()).isEqualTo(TipoPlanMembresia.PREMIUM);
        assertThat(mem.getFechaInicio()).isEqualTo(hoy);
        assertThat(mem.getFechaVencimiento()).isEqualTo(hoy.plusYears(1));
        assertThat(mem.getUltimoPagoId()).isEqualTo(PAGO_ID);
    }

    @Test
    @DisplayName("Idempotencia: mismo pagoId dos veces → no toca la membresía")
    void mismoPagoIdNoModificaNada() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroConMembresia(
                EstadoMembresia.ACTIVA, TipoPlanMembresia.FULL,
                hoy.minusDays(10), hoy.plusDays(20), PAGO_ID); // mismo pagoId
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("FULL", "MENSUAL"));

        Membresia mem = m.getMembresia();
        assertThat(mem.getEstado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(mem.getFechaVencimiento()).isEqualTo(hoy.plusDays(20)); // intacta
        verify(miembroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Miembro sin membresía → no toca nada, no lanza excepción")
    void miembroSinMembresiaNoHaceNada() {
        Miembro m = new Miembro();
        m.setNombre("Sin membresía");
        m.setEmail("sin@example.com");
        // membresia == null
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("BASICO", "MENSUAL"));

        verify(miembroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Plan inválido → descarta sin tocar la membresía")
    void planInvalidoDescarta() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembroConMembresia(
                EstadoMembresia.SUSPENDIDA, TipoPlanMembresia.BASICO,
                hoy, hoy.plusDays(30), null);
        when(miembroRepository.findById(MIEMBRO_ID)).thenReturn(Optional.of(m));

        listener.consumir(evento("PLANINEXISTENTE", "MENSUAL"));

        assertThat(m.getMembresia().getEstado()).isEqualTo(EstadoMembresia.SUSPENDIDA);
        verify(miembroRepository, never()).save(any());
    }
}