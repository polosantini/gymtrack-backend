package com.gymtrack.clases.domain.exception;

public class MembresiaInactivaException extends RuntimeException {
    public MembresiaInactivaException() {
        super("La membresía no está activa");
    }
}
