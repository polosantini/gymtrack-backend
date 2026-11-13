package com.gymtrack.clases.domain.observer;

import com.gymtrack.common.events.ReservaCancelada;

/** Observador del hecho de negocio: una reserva fue cancelada y su transacción confirmada. */
public interface ObservadorReservas {
    void alCancelar(ReservaCancelada evento);
}
