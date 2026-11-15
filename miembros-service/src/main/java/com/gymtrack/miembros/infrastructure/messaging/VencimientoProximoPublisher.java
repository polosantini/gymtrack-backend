package com.gymtrack.miembros.infrastructure.messaging;

import com.gymtrack.common.events.EventosConfig;
import com.gymtrack.common.events.MembresiaProximaVencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class VencimientoProximoPublisher {

    private static final Logger log = LoggerFactory.getLogger(VencimientoProximoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VencimientoProximoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(MembresiaProximaVencer evento) {
        rabbitTemplate.convertAndSend(
                EventosConfig.EXCHANGE,
                EventosConfig.RK_MEMBRESIA_PROXIMA_VENCER,
                evento
        );
        log.info("Evento publicado exchange={} routingKey={} miembroId={}",
                EventosConfig.EXCHANGE,
                EventosConfig.RK_MEMBRESIA_PROXIMA_VENCER,
                evento.miembroId());
    }
}