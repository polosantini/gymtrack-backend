package com.gymtrack.clases.domain.observer;

import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SujetoReservasTest {
    private final ObservadorReservas primero = mock(ObservadorReservas.class);
    private final ObservadorReservas segundo = mock(ObservadorReservas.class);
    private final SujetoReservas sujeto = new SujetoReservas(List.of(primero, segundo));
    private final ReservaCancelada evento = new ReservaCancelada(1L, 2L, LocalDate.of(2026,11,17), true);

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    private void iniciar() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
    @Test
    void notificaATodosSoloDespuesDelCommit() {
        iniciar(); sujeto.notificarCancelacion(evento);
        verifyNoInteractions(primero, segundo);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(primero).alCancelar(evento); verify(segundo).alCancelar(evento);
    }
    @Test
    void rollbackNoNotifica() {
        iniciar(); sujeto.notificarCancelacion(evento);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(primero, segundo);
    }
    @Test
    void rechazaLlamadasSinTransaccionEnVezDePublicarAntesDeTiempo() {
        assertThrows(IllegalStateException.class, () -> sujeto.notificarCancelacion(evento));
        verifyNoInteractions(primero, segundo);
    }
}
