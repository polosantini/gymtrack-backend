package com.gymtrack.pagos.domain.exception;
public class MiembroSinMembresiaException extends RuntimeException {
 public MiembroSinMembresiaException() {super("El miembro no tiene una membresía creada");}
}
