package com.gymtrack.miembros.infrastructure.messaging;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
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

    public PagoRegistradoListener(MiembroRepository miembroRepository) {
        this.miembroRepository = miembroRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_PAGO_REGISTRADO)
    @Transactional
    public void consumir(PagoRegistrado evento) {
        log.info("Evento recibido pagoId={} miembroId={} plan={} periodo={} monto={} estado={}",
                evento.pagoId(), evento.miembroId(), evento.plan(),
                evento.periodo(), evento.monto(), evento.estado());

        Optional<Miembro> opt = miembroRepository.findById(evento.miembroId());
        if (opt.isEmpty()) {
            log.warn("Miembro no encontrado miembroId={} pagoId={} — evento descartado",
                    evento.miembroId(), evento.pagoId());
            return;
        }
        Miembro miembro = opt.get();

        Membresia membresia = miembro.getMembresia();
        if (membresia == null) {
            log.warn("Miembro sin membresía miembroId={} pagoId={} — evento descartado. " +
                            "El flujo correcto exige crear la membresía antes de pagar.",
                    evento.miembroId(), evento.pagoId());
            return;
        }

        if (Objects.equals(membresia.getUltimoPagoId(), evento.pagoId())) {
            log.info("Evento duplicado ignorado pagoId={} miembroId={}",
                    evento.pagoId(), evento.miembroId());
            return;
        }

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

        membresia.setPlan(nuevoPlan);
        membresia.setEstado(EstadoMembresia.ACTIVA);
        membresia.setFechaVencimiento(nuevoVencimiento);
        membresia.setUltimoPagoId(evento.pagoId());

        miembroRepository.save(miembro);

        log.info("Membresía activada/renovada miembroId={} plan={} fechaInicio={} fechaVencimiento={} pagoId={}",
                miembro.getId(), nuevoPlan, membresia.getFechaInicio(),
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