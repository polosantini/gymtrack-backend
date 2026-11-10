package com.gymtrack.clases.domain.exception;

public class MiembrosNoDisponibleException extends RuntimeException {
    public MiembrosNoDisponibleException() {
        super("No se pudo validar la membresía, intente de nuevo");
    }
}
