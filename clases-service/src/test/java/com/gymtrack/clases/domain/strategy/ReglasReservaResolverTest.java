package com.gymtrack.clases.domain.strategy;

import com.gymtrack.clases.domain.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReglasReservaResolverTest {
    private final ReservaRepository repository = mock(ReservaRepository.class);
    private final ReglaBasico basico = new ReglaBasico(repository);
    private final ReglaFull full = new ReglaFull(repository);
    private final ReglaPremium premium = new ReglaPremium();
    private final ReglasReservaResolver resolver = new ReglasReservaResolver(List.of(basico, full, premium));

    @Test
    void eligeCadaRegla() {
        assertSame(basico, resolver.resolver("BASICO"));
        assertSame(full, resolver.resolver("FULL"));
        assertSame(premium, resolver.resolver("PREMIUM"));
    }

    @Test
    void planInvalidoONuloNoTieneReglaPorDefecto() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver("OTRO"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolver(null));
    }

    @Test
    void nuevoPlanSeRegistraSinEditarElResolverOcp() {
        ReglasReservaStrategy nuevo = new ReglasReservaStrategy() {
            public String plan() { return "NUEVO"; }
            public void validar(Long miembroId, LocalDate fecha) { }
        };
        var ampliado = new ReglasReservaResolver(List.of(basico, full, premium, nuevo));
        assertSame(nuevo, ampliado.resolver("NUEVO"));
    }

    @Test
    void noPermiteDosReglasParaElMismoPlan() {
        assertThrows(IllegalStateException.class,
                () -> new ReglasReservaResolver(List.of(premium, new ReglaPremium())));
    }
}
