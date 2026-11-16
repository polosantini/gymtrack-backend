package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import com.gymtrack.pagos.domain.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PagoRegistradoNotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(PagoRegistradoNotificacionListener.class);

    private final NotificacionRepository notificacionRepository;

    public PagoRegistradoNotificacionListener(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES_PAGO)
    @Transactional
    public void consumir(PagoRegistrado evento) {
        log.info("Evento recibido pagoId={} miembroId={} plan={} periodo={} monto={}",
                evento.pagoId(), evento.miembroId(), evento.plan(),
                evento.periodo(), evento.monto());

        // Defensa contra eventos inválidos
        if (evento.pagoId() == null || evento.miembroId() == null) {
            log.warn("Evento sin pagoId o miembroId, se descarta: {}", evento);
            return;
        }

        // Idempotencia
        if (notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CONFIRMACION_PAGO, evento.pagoId())) {
            log.info("Notificación ya existe para pagoId={}, evento duplicado ignorado", evento.pagoId());
            return;
        }

        // Crear notificación
        Notificacion notificacion = new Notificacion();
        notificacion.setMiembroId(evento.miembroId());
        notificacion.setTipo(TipoNotificacion.CONFIRMACION_PAGO);
        notificacion.setMensaje(String.format(
                "Pago registrado: plan %s por periodo %s — $%s",
                evento.plan(), evento.periodo(), evento.monto().toPlainString()));
        notificacion.setReferenciaExterna(evento.pagoId());
        notificacion.setLeida(false);

        Notificacion guardada = notificacionRepository.save(notificacion);

        log.info("Notificación creada id={} miembroId={} pagoId={} tipo={}",
                guardada.getId(), evento.miembroId(), evento.pagoId(), TipoNotificacion.CONFIRMACION_PAGO);
    }
}