package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.clases.web.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaseReservasQueryTest {
    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);
    @Mock private ClaseRepository claseRepository;
    @Mock private ReservaRepository reservaRepository;
    @InjectMocks private ClaseQueryServiceImpl reservaService;
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
    @DisplayName("listarPorClaseYFecha separa confirmados y espera, y calcula cupos libres")
    void listarPorClaseYFecha() {
        Clase clase = spinningAforo3();
        when(claseRepository.findById(1L)).thenReturn(Optional.of(clase));
        when(reservaRepository.findByClaseIdAndFechaAndEstadoOrderByIdAsc(1L, MARTES_17, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reserva(clase, 1L, EstadoReserva.CONFIRMADA, null),
                        reserva(clase, 2L, EstadoReserva.CONFIRMADA, null)));
        when(reservaRepository.findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(1L, MARTES_17, EstadoReserva.EN_ESPERA))
                .thenReturn(List.of(reserva(clase, 4L, EstadoReserva.EN_ESPERA, 1)));

        ReservasClaseResponse respuesta = reservaService.listarPorClaseYFecha(1L, MARTES_17);

        assertEquals(2, respuesta.confirmados().size());
        assertEquals(1, respuesta.enEspera().size());
        assertEquals(1, respuesta.enEspera().get(0).posicionEspera());
        assertEquals(1, respuesta.cuposLibres());
    }

    @Test
    @DisplayName("listarPorClaseYFecha con fecha que no es día de la clase lanza FechaNoCorrespondeException (400)")
    void listarPorClaseYFechaQueNoCorresponde() {
        when(claseRepository.findById(1L)).thenReturn(Optional.of(spinningAforo3()));

        assertThrows(FechaNoCorrespondeException.class,
                () -> reservaService.listarPorClaseYFecha(1L, MARTES_17.plusDays(1)));
    }
}
