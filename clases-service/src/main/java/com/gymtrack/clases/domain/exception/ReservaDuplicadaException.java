package com.gymtrack.clases.domain.exception;

import java.time.LocalDate;

public class ReservaDuplicadaException extends RuntimeException {
    public ReservaDuplicadaException(Long miembroId, Long claseId, LocalDate fecha) {
        super("El miembro " + miembroId + " ya tiene una reserva para la clase " + claseId + " el " + fecha);
    }
}
