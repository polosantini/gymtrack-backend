package com.gymtrack.common.rpc;
/** Solo existencia: una membresia suspendida/vencida tambien se puede pagar para activarla. */
public record RespuestaMembresiaPago(Long miembroId,Boolean existeMiembro,Boolean tieneMembresia) {}
