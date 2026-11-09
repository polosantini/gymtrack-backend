package com.gymtrack.clases.domain.strategy;

import java.time.LocalDate;

/** Regla de reservas de un plan. La fecha es el día de la sesión en America/Bogota. */
public interface ReglasReservaStrategy {

    String plan();

    /** Valida ANTES de consultar el aforo o guardar la nueva reserva. */
    void validar(Long miembroId, LocalDate fecha);
}
