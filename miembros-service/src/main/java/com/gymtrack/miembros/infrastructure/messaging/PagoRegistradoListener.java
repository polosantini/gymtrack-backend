package com.gymtrack.miembros.infrastructure.messaging;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import com.gymtrack.miembros.domain.repository.PagoAplicadoRepository;
import com.gymtrack.miembros.domain.model.PagoAplicado;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class PagoRegistradoListener {

    private static final Logger log = LoggerFactory.getLogger(PagoRegistradoListener.class);

    private static final Set<String> PERIODOS_VALIDOS = Set.of("MENSUAL", "TRIMESTRAL", "ANUAL");

    private final MiembroRepository miembroRepository;
    private final PagoAplicadoRepository pagosAplicados;

    public PagoRegistradoListener(MiembroRepository miembroRepository, PagoAplicadoRepository pagosAplicados) {
        this.pagosAplicados=pagosAplicados;
        this.miembroRepository = miembroRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_PAGO_REGISTRADO)
    @Transactional
    public void consumir(PagoRegistrado evento) {
        if(evento==null || evento.pagoId()==null || evento.miembroId()==null)throw new AmqpRejectAndDontRequeueException("Pago sin identidad");
        if(evento.plan()==null || evento.periodo()==null || evento.monto()==null || evento.monto().signum()<=0)
            throw new AmqpRejectAndDontRequeueException("Pago incompleto o monto invalido");
        if(!"REGISTRADO".equals(evento.estado())) {
            log.warn("Estado de pago no admitido: {}",evento.estado());return;
        }
        log.info("Evento recibido pagoId={} miembroId={} plan={} periodo={} monto={} estado={}",
                evento.pagoId(), evento.miembroId(), evento.plan(),
                evento.periodo(), evento.monto(), evento.estado());

        Optional<Miembro> opt = miembroRepository.findByIdForUpdate(evento.miembroId());
        if (opt.isEmpty()) {
            log.warn("Miembro no encontrado miembroId={} pagoId={} — evento descartado",
                    evento.miembroId(), evento.pagoId());
            throw new AmqpRejectAndDontRequeueException("Miembro inexistente: "+evento.miembroId());
        }
        Miembro miembro = opt.get();

        Membresia membresia = miembro.getMembresia();
        if (membresia == null) {
            log.warn("Miembro sin membresía miembroId={} pagoId={} — evento descartado. " +
                            "El flujo correcto exige crear la membresía antes de pagar.",
                    evento.miembroId(), evento.pagoId());
            throw new AmqpRejectAndDontRequeueException("Crear la membresia antes de pagar: "+evento.miembroId());
        }

        if (pagosAplicados.existsById(evento.pagoId()) || Objects.equals(membresia.getUltimoPagoId(), evento.pagoId())) {
            log.info("Evento duplicado ignorado pagoId={} miembroId={}",
                    evento.pagoId(), evento.miembroId());
            return;
        }

        // Corte de migracion: antes de esta version solo se recordaba el ultimo pago.
        // Un ID antiguo no registrado se conserva en recuperacion para conciliacion, sin sumar credito a ciegas.
        if(membresia.getPagosPreviosHasta()==null)
            membresia.setPagosPreviosHasta(membresia.getUltimoPagoId()==null?0L:membresia.getUltimoPagoId());
        if(evento.pagoId()<=membresia.getPagosPreviosHasta())
            throw new AmqpRejectAndDontRequeueException("Pago anterior al registro durable; revisar antes de reaplicar: "+evento.pagoId());

        TipoPlanMembresia nuevoPlan;
        try {
            nuevoPlan = TipoPlanMembresia.valueOf(evento.plan());
        } catch (IllegalArgumentException ex) {
            log.warn("Plan inválido '{}' en pagoId={} — evento descartado",
                    evento.plan(), evento.pagoId());
            return;
        }
        if (!PERIODOS_VALIDOS.contains(evento.periodo())) {
            log.warn("Periodo inválido '{}' en pagoId={} — evento descartado",
                    evento.periodo(), evento.pagoId());
            return;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate base;
        boolean esRenovacion = membresia.getEstado() == EstadoMembresia.ACTIVA
                && membresia.getFechaVencimiento() != null
                && !membresia.getFechaVencimiento().isBefore(hoy);

        if (esRenovacion) {
            base = membresia.getFechaVencimiento();
            log.info("Renovación detectada miembroId={} base={} periodo={}",
                    miembro.getId(), base, evento.periodo());
        } else {
            base = hoy;
            membresia.setFechaInicio(hoy);
            log.info("Activación detectada miembroId={} estadoAnterior={} inicio={}",
                    miembro.getId(), membresia.getEstado(), hoy);
        }

        LocalDate nuevoVencimiento = calcularVencimiento(base, evento.periodo());

        // Un evento antiguo nunca revierte un cambio de plan posterior; su periodo unico si se acredita.
        if(membresia.getUltimoPagoId()==null || evento.pagoId()>membresia.getUltimoPagoId()) membresia.setPlan(nuevoPlan);
        membresia.setEstado(EstadoMembresia.ACTIVA);
        membresia.setFechaVencimiento(nuevoVencimiento);
        if(membresia.getUltimoPagoId()==null || evento.pagoId()>membresia.getUltimoPagoId()) membresia.setUltimoPagoId(evento.pagoId());
        pagosAplicados.saveAndFlush(new PagoAplicado(evento.pagoId(),evento.miembroId()));

        miembroRepository.save(miembro);

        log.info("Membresía activada/renovada miembroId={} plan={} fechaInicio={} fechaVencimiento={} pagoId={}",
                miembro.getId(), membresia.getPlan(), membresia.getFechaInicio(),
                nuevoVencimiento, evento.pagoId());
    }

    private LocalDate calcularVencimiento(LocalDate base, String periodo) {
        return switch (periodo) {
            case "MENSUAL"    -> base.plusMonths(1);
            case "TRIMESTRAL" -> base.plusMonths(3);
            case "ANUAL"      -> base.plusYears(1);
            default -> throw new IllegalStateException("Periodo no validado: " + periodo);
        };
    }
}