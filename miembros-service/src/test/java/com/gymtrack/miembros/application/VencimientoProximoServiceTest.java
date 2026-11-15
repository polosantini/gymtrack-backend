package com.gymtrack.miembros.application;

import com.gymtrack.common.events.MembresiaProximaVencer;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import com.gymtrack.miembros.infrastructure.messaging.VencimientoProximoPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VencimientoProximoService — publica y marca notificado")
class VencimientoProximoServiceTest {

    @Mock
    private MiembroRepository miembroRepository;

    @Mock
    private VencimientoProximoPublisher publisher;

    private VencimientoProximoService service;

    @BeforeEach
    void setUp() {
        service = new VencimientoProximoService(miembroRepository, publisher);
        ReflectionTestUtils.setField(service, "ventanaDiasDefault", 7);
    }

    private Miembro miembro(Long id, String nombre, LocalDate vencimiento, LocalDate notificadoEl) {
        Membresia mem = new Membresia();
        mem.setEstado(EstadoMembresia.ACTIVA);
        mem.setPlan(TipoPlanMembresia.FULL);
        mem.setFechaInicio(vencimiento.minusDays(30));
        mem.setFechaVencimiento(vencimiento);
        mem.setNotificadoVencimientoEl(notificadoEl);

        Miembro m = new Miembro();
        ReflectionTestUtils.setField(m, "id", id);
        m.setNombre(nombre);
        m.setEmail(nombre.toLowerCase() + "@test.com");
        m.setMembresia(mem);
        return m;
    }

    @Test
    @DisplayName("Publica evento y marca notificadoEl=hoy para miembros en ventana")
    void publicaYMarcaMiembrosEnVentana() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembro(1L, "Diego", hoy.plusDays(3), null);
        when(miembroRepository.findMiembrosConMembresiaVenciendoEnVentana(
                eq(EstadoMembresia.ACTIVA), eq(hoy), eq(hoy.plusDays(7))))
                .thenReturn(List.of(m));

        int publicados = service.procesar(null);

        assertThat(publicados).isEqualTo(1);
        ArgumentCaptor<MembresiaProximaVencer> captor = ArgumentCaptor.forClass(MembresiaProximaVencer.class);
        verify(publisher).publicar(captor.capture());
        MembresiaProximaVencer evento = captor.getValue();
        assertThat(evento.miembroId()).isEqualTo(1L);
        assertThat(evento.nombre()).isEqualTo("Diego");
        assertThat(evento.fechaVencimiento()).isEqualTo(hoy.plusDays(3));
        assertThat(evento.diasRestantes()).isEqualTo(3);
        assertThat(m.getMembresia().getNotificadoVencimientoEl()).isEqualTo(hoy);
        verify(miembroRepository).save(m);
    }

    @Test
    @DisplayName("No publica para miembros ya notificados hoy")
    void noPublicaSiYaNotificadoHoy() {
        LocalDate hoy = LocalDate.now();
        Miembro m = miembro(1L, "Diego", hoy.plusDays(3), hoy);
        when(miembroRepository.findMiembrosConMembresiaVenciendoEnVentana(
                eq(EstadoMembresia.ACTIVA), eq(hoy), eq(hoy.plusDays(7))))
                .thenReturn(List.of(m));

        int publicados = service.procesar(null);

        assertThat(publicados).isZero();
        verify(publisher, never()).publicar(any());
        verify(miembroRepository, never()).save(any());
    }

    @Test
    @DisplayName("ventanaDias custom sobrescribe el default")
    void ventanaCustomSobrescribeDefault() {
        LocalDate hoy = LocalDate.now();
        when(miembroRepository.findMiembrosConMembresiaVenciendoEnVentana(
                eq(EstadoMembresia.ACTIVA), eq(hoy), eq(hoy.plusDays(365))))
                .thenReturn(List.of());

        service.procesar(365);

        verify(miembroRepository).findMiembrosConMembresiaVenciendoEnVentana(
                EstadoMembresia.ACTIVA, hoy, hoy.plusDays(365));
    }
}