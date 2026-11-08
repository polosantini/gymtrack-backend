package com.gymtrack.pagos.domain.exception;

public class PeriodoInvalidoException extends RuntimeException{
    public PeriodoInvalidoException(String periodo) {
        super("El periodo "+ periodo +" no es válido. Valores permitidos: MENSUAL, TRIMESTRAL, ANUAL");
    }
}
