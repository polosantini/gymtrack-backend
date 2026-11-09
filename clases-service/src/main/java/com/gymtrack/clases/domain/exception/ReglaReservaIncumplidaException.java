package com.gymtrack.clases.domain.exception;

public class ReglaReservaIncumplidaException extends RuntimeException {
    public ReglaReservaIncumplidaException(String plan, int limite, String periodo) {
        super("El plan " + plan + " permite máximo " + limite + " reservas por " + periodo);
    }
}
