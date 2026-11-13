package com.gymtrack.clases.infrastructure.messaging;

import com.gymtrack.clases.application.port.PublicadorAsignacionPendiente;
import com.gymtrack.clases.domain.repository.CancelacionProcesadaRepository;
import com.gymtrack.common.events.EventosConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Solo se llama después del commit de negocio; un fallo deja el aviso pendiente para la redelivery. */
@Component
public class PublicadorCupoPendiente implements PublicadorAsignacionPendiente {
    private final CancelacionProcesadaRepository procesadas;
    private final RabbitTemplate rabbit;
    public PublicadorCupoPendiente(CancelacionProcesadaRepository procesadas, RabbitTemplate rabbit) {
        this.procesadas = procesadas; this.rabbit = rabbit;
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publicar(Long reservaCanceladaId) {
        var recibo = procesadas.findByIdForUpdate(reservaCanceladaId)
                .orElseThrow(() -> new IllegalStateException("No existe recibo de cancelación"));
        var evento = recibo.eventoPendiente();
        if (evento == null) return;
        rabbit.convertAndSend(EventosConfig.EXCHANGE, EventosConfig.RK_RESERVA_CUPO_ASIGNADO, evento, mensaje -> {
            // Identidad estable en metadatos AMQP; no se añaden campos al JSON del contrato.
            mensaje.getMessageProperties().setMessageId("reserva.cupo.asignado:" + reservaCanceladaId);
            return mensaje;
        });
        recibo.marcarEnviado();
    }
}
