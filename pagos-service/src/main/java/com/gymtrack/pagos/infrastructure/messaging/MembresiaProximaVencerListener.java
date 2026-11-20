package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.MembresiaProximaVencer;
import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.EventoNotificacionProcesado;
import com.gymtrack.pagos.domain.repository.EventoNotificacionProcesadoRepository;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import com.gymtrack.pagos.domain.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MembresiaProximaVencerListener {

    private static final Logger log = LoggerFactory.getLogger(MembresiaProximaVencerListener.class);

    private final NotificacionRepository notificacionRepository;
    private final EventoNotificacionProcesadoRepository procesados;

    public MembresiaProximaVencerListener(NotificacionRepository notificacionRepository, EventoNotificacionProcesadoRepository procesados) {
        this.procesados=procesados;
        this.notificacionRepository = notificacionRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES_VENCIMIENTO)
    @Transactional
    public void consumir(MembresiaProximaVencer evento) {
        log.info("Evento recibido miembroId={} nombre={} fechaVencimiento={} diasRestantes={}",
                evento.miembroId(), evento.nombre(), evento.fechaVencimiento(), evento.diasRestantes());

        if (evento.miembroId() == null) {
            log.warn("Evento sin miembroId, se descarta: {}", evento);
            return;
        }

        if(evento.fechaVencimiento()==null)throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Vencimiento sin fecha");
        String clave="vencimiento:"+evento.miembroId()+":"+evento.fechaVencimiento()+":"+evento.diasRestantes();
        if(procesados.existsById(clave))return;
        Notificacion notificacion = new Notificacion();
        notificacion.setMiembroId(evento.miembroId());
        notificacion.setTipo(TipoNotificacion.VENCIMIENTO_PROXIMO);
        notificacion.setMensaje(String.format(
                "Tu membresía vence en %d día(s) (%s)",
                evento.diasRestantes(), evento.fechaVencimiento()));
        notificacion.setLeida(false);

        procesados.saveAndFlush(new EventoNotificacionProcesado(clave));
        Notificacion guardada = notificacionRepository.save(notificacion);

        log.info("Notificación creada id={} miembroId={} tipo=VENCIMIENTO_PROXIMO",
                guardada.getId(), evento.miembroId());
    }
}