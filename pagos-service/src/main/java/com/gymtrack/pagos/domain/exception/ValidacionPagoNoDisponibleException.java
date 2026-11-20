package com.gymtrack.pagos.domain.exception;
public class ValidacionPagoNoDisponibleException extends RuntimeException {
 public ValidacionPagoNoDisponibleException() {super("No se pudo validar la membresía para registrar el pago, intente de nuevo");}
}
