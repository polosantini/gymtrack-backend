package com.gymtrack.clases.application;

import com.gymtrack.clases.application.port.MiembrosClient;
import com.gymtrack.clases.application.port.AccesoMembresia;
import com.gymtrack.clases.domain.strategy.ReglasReservaResolver;
import com.gymtrack.clases.domain.strategy.ReglaPremium;
import org.junit.jupiter.api.BeforeEach;
import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.EstadoReservaInvalidoException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.exception.ReservaNotFoundException;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.DiaSemana;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    // 2026-11-17 es martes
    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MiembrosClient miembrosClient;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void prepararServicio() {
        reservaService = new ReservaServiceImpl(claseRepository, reservaRepository, miembrosClient,
                new ReglasReservaResolver(List.of(new ReglaPremium())),
                new ReservarClaseUseCaseFactory(claseRepository, reservaRepository));
    }

    private void miembroActivo(Long miembroId) {
        when(miembrosClient.consultarAcceso(miembroId)).thenReturn(new AccesoMembresia(true, "PREMIUM"));
    }

    private void bloqueoCancelacion(Long reservaId) {
        when(reservaRepository.findClaseIdByReservaId(reservaId)).thenReturn(Optional.of(1L));
        when(claseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(spinningAforo3()));
    }

    private Clase spinningAforo3() {
        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", 1L);
        clase.setNombre("Spinning");
        clase.setInstructor("Carlos");
        clase.setDias(Set.of(DiaSemana.MARTES, DiaSemana.JUEVES));
        clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45);
        clase.setAforo(3);
        clase.setSala("Sala 1");
        return clase;
    }

    private void claseExisteSinDuplicado(Long miembroId) {
        miembroActivo(miembroId);
        when(claseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(spinningAforo3()));
        when(reservaRepository.existsByMiembroIdAndClaseIdAndFechaAndEstadoNot(
                miembroId, 1L, MARTES_17, EstadoReserva.CANCELADA)).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Con cupo disponible la reserva queda CONFIRMADA y sin posición de espera")
    void conCupoQuedaConfirmada() {
        claseExisteSinDuplicado(10L);
        when(reservaRepository.countByClaseIdAndFechaAndEstado(1L, MARTES_17, EstadoReserva.CONFIRMADA)).thenReturn(2L);

        ReservaResponse respuesta = reservaService.reservar(new ReservaRequest(10L, 1L, MARTES_17));

        assertEquals("CONFIRMADA", respuesta.estado());
        assertNull(respuesta.posicionEspera());
        assertEquals("Spinning", respuesta.claseNombre());
    }

    @Test
    @DisplayName("Sin cupo la reserva queda EN_ESPERA con posición = en espera existentes + 1")
    void sinCupoQuedaEnEspera() {
        claseExisteSinDuplicado(10L);
        when(reservaRepository.countByClaseIdAndFechaAndEstado(1L, MARTES_17, EstadoReserva.CONFIRMADA)).thenReturn(3L);
        when(reservaRepository.countByClaseIdAndFechaAndEstado(1L, MARTES_17, EstadoReserva.EN_ESPERA)).thenReturn(1L);
        when(reservaRepository.maxPosicionEspera(1L, MARTES_17, EstadoReserva.EN_ESPERA)).thenReturn(1);

        ReservaResponse respuesta = reservaService.reservar(new ReservaRequest(10L, 1L, MARTES_17));

        assertEquals("EN_ESPERA", respuesta.estado());
        assertEquals(2, respuesta.posicionEspera());
    }

    @Test
    @DisplayName("Clase inexistente lanza ClaseNotFoundException (404)")
    void claseInexistente() {
        miembroActivo(10L);
        when(claseRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ClaseNotFoundException.class,
                () -> reservaService.reservar(new ReservaRequest(10L, 99L, MARTES_17)));
    }

    @Test
    @DisplayName("Fecha que no es día de la clase lanza FechaNoCorrespondeException (400)")
    void fechaQueNoCorresponde() {
        miembroActivo(10L);
        when(claseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(spinningAforo3()));
        LocalDate miercoles = MARTES_17.plusDays(1);

        assertThrows(FechaNoCorrespondeException.class,
                () -> reservaService.reservar(new ReservaRequest(10L, 1L, miercoles)));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    private Reserva reserva(Clase clase, Long miembroId, EstadoReserva estado, Integer posicion) {
        Reserva r = new Reserva();
        r.setClase(clase);
        r.setMiembroId(miembroId);
        r.setFecha(MARTES_17);
        r.setEstado(estado);
        r.setPosicionEspera(posicion);
        return r;
    }



    @Test
    @DisplayName("listarPorMiembro devuelve el historial con asistencia")
    void listarPorMiembro() {
        Clase clase = spinningAforo3();
        Reserva asistio = reserva(clase, 1L, EstadoReserva.CONFIRMADA, null);
        asistio.setAsistencia(true);
        when(reservaRepository.findByMiembroIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(asistio, reserva(clase, 1L, EstadoReserva.CANCELADA, null)));

        List<ReservaResponse> historial = reservaService.listarPorMiembro(1L);

        assertEquals(2, historial.size());
        assertEquals(Boolean.TRUE, historial.get(0).asistencia());
        assertEquals("CANCELADA", historial.get(1).estado());
        assertNull(historial.get(1).asistencia());
    }

    // ---------- PATCH /api/reservas/{id}/cancelar ----------

    @Test
    @DisplayName("cancelar una reserva CONFIRMADA la deja CANCELADA")
    void cancelarConfirmada() {
        bloqueoCancelacion(1L);
        Reserva confirmada = reserva(spinningAforo3(), 1L, EstadoReserva.CONFIRMADA, null);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(confirmada));

        ReservaResponse respuesta = reservaService.cancelar(1L);

        assertEquals("CANCELADA", respuesta.estado());
        assertNull(respuesta.posicionEspera());
        assertEquals(EstadoReserva.CANCELADA, confirmada.getEstado());
    }

    @Test
    @DisplayName("cancelar una reserva EN_ESPERA conserva las posiciones de los demás")
    void cancelarEnEsperaConservaPosiciones() {
        bloqueoCancelacion(4L);
        Clase clase = spinningAforo3();
        Reserva primera = reserva(clase, 4L, EstadoReserva.EN_ESPERA, 1);
        Reserva segunda = reserva(clase, 5L, EstadoReserva.EN_ESPERA, 2);
        Reserva tercera = reserva(clase, 6L, EstadoReserva.EN_ESPERA, 3);
        when(reservaRepository.findById(4L)).thenReturn(Optional.of(primera));


        ReservaResponse respuesta = reservaService.cancelar(4L);

        assertEquals("CANCELADA", respuesta.estado());
        assertNull(respuesta.posicionEspera());
        assertEquals(2, segunda.getPosicionEspera());
        assertEquals(3, tercera.getPosicionEspera());
        verify(reservaRepository, never()).findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(
                1L, MARTES_17, EstadoReserva.EN_ESPERA);
    }

    @Test
    @DisplayName("cancelar una reserva ya CANCELADA lanza EstadoReservaInvalidoException (409)")
    void cancelarYaCancelada() {
        bloqueoCancelacion(1L);
        when(reservaRepository.findById(1L))
                .thenReturn(Optional.of(reserva(spinningAforo3(), 1L, EstadoReserva.CANCELADA, null)));

        assertThrows(EstadoReservaInvalidoException.class, () -> reservaService.cancelar(1L));
    }

    @Test
    @DisplayName("cancelar una reserva inexistente lanza ReservaNotFoundException (404)")
    void cancelarInexistente() {
        when(reservaRepository.findClaseIdByReservaId(99L)).thenReturn(Optional.empty());

        assertThrows(ReservaNotFoundException.class, () -> reservaService.cancelar(99L));
    }

    // ---------- PATCH /api/reservas/{id}/asistencia ----------

    @Test
    @DisplayName("marcarAsistencia guarda asistio en una reserva CONFIRMADA")
    void marcarAsistenciaConfirmada() {
        Reserva confirmada = reserva(spinningAforo3(), 1L, EstadoReserva.CONFIRMADA, null);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(confirmada));

        ReservaResponse respuesta = reservaService.marcarAsistencia(1L, true);

        assertEquals(Boolean.TRUE, respuesta.asistencia());
        assertEquals(Boolean.TRUE, confirmada.getAsistencia());
    }

    @Test
    @DisplayName("marcarAsistencia en una reserva EN_ESPERA lanza EstadoReservaInvalidoException (409)")
    void marcarAsistenciaEnEspera() {
        when(reservaRepository.findById(4L))
                .thenReturn(Optional.of(reserva(spinningAforo3(), 4L, EstadoReserva.EN_ESPERA, 1)));

        assertThrows(EstadoReservaInvalidoException.class, () -> reservaService.marcarAsistencia(4L, true));
    }

    @Test
    @DisplayName("marcarAsistencia en una reserva inexistente lanza ReservaNotFoundException (404)")
    void marcarAsistenciaInexistente() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservaNotFoundException.class, () -> reservaService.marcarAsistencia(99L, false));
    }

    @Test
    @DisplayName("Reserva repetida del mismo miembro, clase y fecha lanza ReservaDuplicadaException (409)")
    void reservaDuplicada() {
        miembroActivo(10L);
        when(claseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(spinningAforo3()));
        when(reservaRepository.existsByMiembroIdAndClaseIdAndFechaAndEstadoNot(
                10L, 1L, MARTES_17, EstadoReserva.CANCELADA)).thenReturn(true);

        assertThrows(ReservaDuplicadaException.class,
                () -> reservaService.reservar(new ReservaRequest(10L, 1L, MARTES_17)));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }
}
