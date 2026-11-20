package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.ReservaCupoAsignado;
import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.EventoNotificacionProcesado;
import com.gymtrack.pagos.domain.repository.EventoNotificacionProcesadoRepository;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import com.gymtrack.pagos.domain.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Component
public class ReservaCupoAsignadoListener {

    private static final Logger log = LoggerFactory.getLogger(ReservaCupoAsignadoListener.class);
    private static final String PREFIJO_MESSAGE_ID = "reserva.cupo.asignado:";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final EventoNotificacionProcesadoRepository procesados;

    public ReservaCupoAsignadoListener(NotificacionRepository notificacionRepository, EventoNotificacionProcesadoRepository procesados) {
        this.procesados=procesados;
        this.notificacionRepository = notificacionRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES_CUPO_ASIGNADO)
    @Transactional
    public void consumir(ReservaCupoAsignado evento, Message mensaje) {
        log.info("Evento recibido miembroId={} claseNombre={} fecha={} hora={}",
                evento.miembroId(), evento.claseNombre(), evento.fecha(), evento.hora());

        if (evento.miembroId() == null) {
            log.warn("Evento sin miembroId, se descarta: {}", evento);
            return;
        }

        if(evento.claseNombre()==null || evento.fecha()==null || evento.hora()==null)
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Asignacion incompleta");
        Long reservaCanceladaId = extraerReservaCanceladaId(mensaje);
        if(reservaCanceladaId==null)throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Asignacion sin messageId estable");
        String clave="cupo:"+reservaCanceladaId;
        if(procesados.existsById(clave))return;

        if (reservaCanceladaId != null
                && notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CUPO_LIBERADO, reservaCanceladaId)) {
            log.info("Notificación CUPO_LIBERADO ya existe para reservaCanceladaId={}, duplicado ignorado",
                    reservaCanceladaId);
            return;
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setMiembroId(evento.miembroId());
        notificacion.setTipo(TipoNotificacion.CUPO_LIBERADO);
        notificacion.setMensaje(String.format(
                "Se liberó un cupo en %s el %s a las %s",
                evento.claseNombre(),
                evento.fecha(),
                evento.hora().format(FORMATO_HORA)));
        notificacion.setReferenciaExterna(reservaCanceladaId);
        notificacion.setLeida(false);

        procesados.saveAndFlush(new EventoNotificacionProcesado(clave));
        Notificacion guardada = notificacionRepository.save(notificacion);

        log.info("Notificación creada id={} miembroId={} tipo=CUPO_LIBERADO reservaCanceladaId={}",
                guardada.getId(), evento.miembroId(), reservaCanceladaId);
    }

    private Long extraerReservaCanceladaId(Message mensaje) {
        if (mensaje == null) return null;
        String messageId = mensaje.getMessageProperties().getMessageId();
        if (messageId == null || !messageId.startsWith(PREFIJO_MESSAGE_ID)) {
            log.warn("messageId ausente o con formato inesperado: '{}'. Se omite idempotencia.",
                    messageId);
            return null;
        }
        try {
            return Long.parseLong(messageId.substring(PREFIJO_MESSAGE_ID.length()));
        } catch (NumberFormatException e) {
            log.warn("messageId no contiene un id numérico: '{}'", messageId);
            return null;
        }
    }
}