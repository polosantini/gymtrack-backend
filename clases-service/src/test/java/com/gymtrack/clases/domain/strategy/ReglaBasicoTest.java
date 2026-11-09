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
class ReglaBasicoTest {
    @Mock private ReservaRepository repository;
    private final LocalDate lunes = LocalDate.of(2026, 11, 16);
    private final LocalDate domingo = lunes.plusDays(6);
    private final Set<EstadoReserva> contables = Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_ESPERA);

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void permitePrimeraYSegunda(long existentes) {
        when(repository.countByMiembroIdAndFechaBetweenAndEstadoIn(10L, lunes, domingo, contables))
                .thenReturn(existentes);
        assertDoesNotThrow(() -> new ReglaBasico(repository).validar(10L, lunes.plusDays(1)));
    }

    @ParameterizedTest
    @ValueSource(longs = {2, 3, 50})
    void rechazaTerceraYPosteriores(long existentes) {
        when(repository.countByMiembroIdAndFechaBetweenAndEstadoIn(10L, lunes, domingo, contables))
                .thenReturn(existentes);
        var ex = assertThrows(ReglaReservaIncumplidaException.class,
                () -> new ReglaBasico(repository).validar(10L, domingo));
        assertEquals("El plan BASICO permite máximo 2 reservas por semana", ex.getMessage());
    }

    @Test
    void lunesYDomingoPertenecenALaMismaSemana() {
        var regla = new ReglaBasico(repository);
        regla.validar(10L, lunes);
        regla.validar(10L, domingo);
        verify(repository, times(2)).countByMiembroIdAndFechaBetweenAndEstadoIn(10L, lunes, domingo, contables);
    }

    @Test
    void lunesSiguienteReiniciaLaVentana() {
        new ReglaBasico(repository).validar(10L, lunes.plusDays(7));
        verify(repository).countByMiembroIdAndFechaBetweenAndEstadoIn(10L, lunes.plusDays(7), domingo.plusDays(7), contables);
    }

    @Test
    void semanaPuedeCruzarDeAnio() {
        new ReglaBasico(repository).validar(10L, LocalDate.of(2027, 1, 1));
        verify(repository).countByMiembroIdAndFechaBetweenAndEstadoIn(10L,
                LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3), contables);
    }
}
