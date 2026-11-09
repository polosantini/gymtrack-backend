package com.gymtrack.clases.domain.exception;

/** La operación no se puede hacer con la reserva en su estado actual (por ejemplo, cancelar una ya cancelada). */
public class EstadoReservaInvalidoException extends RuntimeException {
    public EstadoReservaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
