package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.EventosConfig;
import com.gymtrack.common.events.PagoRegistrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PagoRegistradoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoRegistradoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PagoRegistradoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(PagoRegistrado evento) {
        rabbitTemplate.convertAndSend(
                EventosConfig.EXCHANGE,
                EventosConfig.RK_PAGO_REGISTRADO,
                evento
        );
        log.info("Evento publicado exchange={} routingKey={} pagoId={} miembroId={}",
                EventosConfig.EXCHANGE,
                EventosConfig.RK_PAGO_REGISTRADO,
                evento.pagoId(),
                evento.miembroId());
    }
}