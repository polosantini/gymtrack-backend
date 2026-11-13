package com.gymtrack.clases.infrastructure.messaging;

import com.gymtrack.clases.domain.observer.ObservadorReservas;
import com.gymtrack.common.events.EventosConfig;
import com.gymtrack.common.events.ReservaCancelada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicadorReservaCancelada implements ObservadorReservas {
    private static final Logger log = LoggerFactory.getLogger(PublicadorReservaCancelada.class);
    private final RabbitTemplate rabbitTemplate;

    public PublicadorReservaCancelada(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void alCancelar(ReservaCancelada evento) {
        rabbitTemplate.convertAndSend(EventosConfig.EXCHANGE, EventosConfig.RK_RESERVA_CANCELADA, evento);
        log.info("Enviado a RabbitMQ exchange={} routingKey={} evento={}",
                EventosConfig.EXCHANGE, EventosConfig.RK_RESERVA_CANCELADA, evento);
    }
}
