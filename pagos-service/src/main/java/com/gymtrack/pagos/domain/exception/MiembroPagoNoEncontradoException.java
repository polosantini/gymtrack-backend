package com.gymtrack.pagos.domain.exception;
public class MiembroPagoNoEncontradoException extends RuntimeException {
 public MiembroPagoNoEncontradoException() {super("El miembro no existe");}
}
