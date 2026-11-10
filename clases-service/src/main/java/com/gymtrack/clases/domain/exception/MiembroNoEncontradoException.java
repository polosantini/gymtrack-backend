package com.gymtrack.clases.domain.exception;

public class MiembroNoEncontradoException extends RuntimeException {
    public MiembroNoEncontradoException() {
        super("Miembro no encontrado");
    }
}
