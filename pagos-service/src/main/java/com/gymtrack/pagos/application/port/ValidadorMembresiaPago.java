package com.gymtrack.pagos.application.port;
/** Fuente autoritativa externa. Debe fallar antes de persistir si no existe membresia. */
public interface ValidadorMembresiaPago {void validar(Long miembroId);}
