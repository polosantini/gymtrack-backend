package com.gymtrack.miembros.domain.exception;

public class PlanInvalidoException extends RuntimeException {
    public PlanInvalidoException(String plan) {
        super("El plan " + plan + " no es válido. Valores permitidos: BASICO, FULL, PREMIUM");
    }
}
