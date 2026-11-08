package com.gymtrack.miembros.domain.exception;

public class MiembroNotFoundException extends RuntimeException {
    public MiembroNotFoundException(Long id) {
        super("Miembro no encontrado con id: "+id);
    }
}
