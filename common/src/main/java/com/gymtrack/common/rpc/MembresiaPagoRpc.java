package com.gymtrack.common.rpc;
/** Consulta privada request/reply. No altera el exchange ni los eventos de negocio. */
public final class MembresiaPagoRpc {
 public static final String EXCHANGE="gymtrack.consultas";
 public static final String ROUTING_KEY="miembro.membresia.validar.pago";
 public static final String QUEUE="miembros.validar.membresia.pago.rpc";
 private MembresiaPagoRpc() {}
}
