package com.gymtrack.common.events;

/**
 * Nombres compartidos de la mensajería de GymTrack (RabbitMQ).
 *
 * <p>Todos los servicios publican y consumen sobre el mismo exchange de tipo
 * <em>topic</em>, {@value #EXCHANGE}. Cada evento tiene su propia routing key.
 * Usar siempre estas constantes en lugar de escribir los textos a mano, para
 * que productor y consumidor no se desincronicen.</p>
 */
public final class EventosConfig {

    /** Exchange (tipo topic) por el que viajan todos los eventos. */
    public static final String EXCHANGE = "gymtrack.events";

    /** Routing key de {@link PagoRegistrado}. */
    public static final String RK_PAGO_REGISTRADO = "pago.registrado";

    /** Routing key de {@link MembresiaProximaVencer}. */
    public static final String RK_MEMBRESIA_PROXIMA_VENCER = "membresia.proxima-vencer";

    /** Routing key de {@link ReservaCancelada}. */
    public static final String RK_RESERVA_CANCELADA = "reserva.cancelada";

    /** Routing key de {@link ReservaCupoAsignado}. */
    public static final String RK_RESERVA_CUPO_ASIGNADO = "reserva.cupo-asignado";

    private EventosConfig() {
        // Solo constantes: no se instancia.
    }
}
