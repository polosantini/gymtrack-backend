package com.gymtrack.clases.domain.strategy;

import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReglaFullTest {
    @Mock private ReservaRepository repository;
    private final LocalDate fecha = LocalDate.of(2026, 11, 17);
    private final Set<EstadoReserva> contables = Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_ESPERA);

    @Test
    void permitePrimera() {
        assertDoesNotThrow(() -> new ReglaFull(repository).validar(10L, fecha));
        verify(repository).countByMiembroIdAndFechaBetweenAndEstadoIn(10L, fecha, fecha, contables);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 50})
    void rechazaSegundaYPosteriores(long existentes) {
        when(repository.countByMiembroIdAndFechaBetweenAndEstadoIn(10L, fecha, fecha, contables))
                .thenReturn(existentes);
        var ex = assertThrows(ReglaReservaIncumplidaException.class,
                () -> new ReglaFull(repository).validar(10L, fecha));
        assertEquals("El plan FULL permite máximo 1 reservas por día", ex.getMessage());
    }

    @Test
    void diaSiguienteUsaOtraVentana() {
        new ReglaFull(repository).validar(10L, fecha.plusDays(1));
        verify(repository).countByMiembroIdAndFechaBetweenAndEstadoIn(10L, fecha.plusDays(1), fecha.plusDays(1), contables);
    }
}
