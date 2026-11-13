package com.gymtrack.clases.infrastructure.messaging;

import com.gymtrack.clases.application.AsignarCupoService;
import com.gymtrack.common.events.ReservaCancelada;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorReservaCancelada {
    private final AsignarCupoService servicio;
    public ConsumidorReservaCancelada(AsignarCupoService servicio) { this.servicio = servicio; }

    @RabbitListener(queues = ReservasAmqpConfig.COLA_CANCELACIONES,
            autoStartup = "${clases.eventos.reserva-cancelada.auto-startup:true}")
    public void consumir(ReservaCancelada evento) {
        try {
            servicio.procesar(evento);
        } catch (IllegalArgumentException ex) {
            // Un evento inválido no se arregla con reintentos: conservarlo en la DLQ.
            throw new AmqpRejectAndDontRequeueException("Evento reserva.cancelada inválido", ex);
        }
    }
}
