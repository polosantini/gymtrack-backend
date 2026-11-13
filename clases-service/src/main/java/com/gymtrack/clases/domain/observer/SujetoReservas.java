package com.gymtrack.clases.domain.observer;

import com.gymtrack.common.events.ReservaCancelada;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;

/** Subject de Observer: difiere la notificación hasta el commit de la cancelación. */
@Component
public class SujetoReservas {
    private final List<ObservadorReservas> observadores;

    public SujetoReservas(List<ObservadorReservas> observadores) {
        this.observadores = List.copyOf(observadores);
    }

    public void notificarCancelacion(ReservaCancelada evento) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("La cancelación debe notificarse dentro de una transacción activa");
        }
        // El record conserva los datos originales sin depender de entidades JPA tras el commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                observadores.forEach(observador -> observador.alCancelar(evento));
            }
        });
    }
}
