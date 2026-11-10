package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.domain.strategy.ReglasReservaStrategy;
import com.gymtrack.clases.web.dto.ReservaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservarClaseUseCaseTest {
    @Mock private ClaseRepository clases;
    @Mock private ReservaRepository reservas;
    @Mock private ReglasReservaStrategy regla;
    private ReservarClaseUseCase caso;
    private final LocalDate fecha = LocalDate.of(2026, 11, 17);

    @BeforeEach
    void preparar() {
        caso = new ReservarClaseUseCaseFactory(clases, reservas).crear(regla);
        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", 1L);
        clase.setNombre("Prueba"); clase.setAforo(1); clase.setHora(LocalTime.of(18, 0));
        clase.setDias(Set.of(DiaSemana.MARTES));
        when(clases.findByIdForUpdate(1L)).thenReturn(Optional.of(clase));
    }

    @Test
    void cualquierEstrategiaInyectadaSeEjecutaAntesDelAforo() {
        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        Reserva resultado = caso.ejecutar(new ReservaRequest(10L, 1L, fecha));
        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        var orden = inOrder(clases, regla, reservas);
        orden.verify(clases).findByIdForUpdate(1L);
        orden.verify(regla).validar(10L, fecha);
        orden.verify(reservas).countByClaseIdAndFechaAndEstado(1L, fecha, EstadoReserva.CONFIRMADA);
        verify(regla, never()).plan(); // El caso de uso no resuelve el plan.
    }

    @Test
    void siLaEstrategiaRechazaNoConsultaAforoNiGuarda() {
        doThrow(new ReglaReservaIncumplidaException("FALSA", 0, "día")).when(regla).validar(10L, fecha);
        assertThrows(ReglaReservaIncumplidaException.class,
                () -> caso.ejecutar(new ReservaRequest(10L, 1L, fecha)));
        verify(reservas, never()).countByClaseIdAndFechaAndEstado(any(), any(), any());
        verify(reservas, never()).save(any());
    }
}
