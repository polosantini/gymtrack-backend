package com.gymtrack.clases.domain.exception;

public class ReservaNotFoundException extends RuntimeException {
    public ReservaNotFoundException(Long id) {
        super("Reserva no encontrada con id: " + id);
    }
}
