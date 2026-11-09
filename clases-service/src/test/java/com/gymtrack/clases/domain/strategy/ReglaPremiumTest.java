package com.gymtrack.clases.domain.strategy;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReglaPremiumTest {
    @Test
    void noTieneLimiteNiNecesitaConsultarElRepositorio() {
        ReglasReservaStrategy regla = new ReglaPremium();
        assertEquals("PREMIUM", regla.plan());
        for (int i = 0; i < 1000; i++) {
            assertDoesNotThrow(() -> regla.validar(10L, LocalDate.of(2026, 11, 17)));
        }
    }
}
