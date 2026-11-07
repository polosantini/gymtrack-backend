package com.gymtrack.miembros.domain.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException(String email) {
        super("Ya existe un miembro con el email "+email);
    }
}
