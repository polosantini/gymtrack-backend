package com.gymtrack.miembros.application;

import com.gymtrack.common.events.MembresiaProximaVencer;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import com.gymtrack.miembros.infrastructure.messaging.VencimientoProximoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class VencimientoProximoService {

    private static final Logger log = LoggerFactory.getLogger(VencimientoProximoService.class);

    private final MiembroRepository miembroRepository;
    private final VencimientoProximoPublisher publisher;

    @Value("${gymtrack.vencimientos.ventana-dias:7}")
    private int ventanaDiasDefault;

    public VencimientoProximoService(MiembroRepository miembroRepository,
                                     VencimientoProximoPublisher publisher) {
        this.miembroRepository = miembroRepository;
        this.publisher = publisher;
    }

    @Transactional
    public int procesar(Integer ventanaDias) {
        int dias = (ventanaDias != null) ? ventanaDias : ventanaDiasDefault;
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(dias);

        List<Miembro> candidatos = miembroRepository.findMiembrosConMembresiaVenciendoEnVentana(
                EstadoMembresia.ACTIVA, hoy, hasta);

        int publicados = 0;
        for (Miembro miembro : candidatos) {
            Membresia membresia = miembro.getMembresia();

            if (hoy.equals(membresia.getNotificadoVencimientoEl())) {
                log.debug("Miembro {} ya notificado hoy, se omite", miembro.getId());
                continue;
            }

            long diasRestantes = ChronoUnit.DAYS.between(hoy, membresia.getFechaVencimiento());

            MembresiaProximaVencer evento = new MembresiaProximaVencer(
                    miembro.getId(),
                    miembro.getNombre(),
                    membresia.getFechaVencimiento(),
                    (int) diasRestantes
            );

            publisher.publicar(evento);
            membresia.setNotificadoVencimientoEl(hoy);
            miembroRepository.save(miembro);
            publicados++;

            log.info("Evento membresia.proxima.vencer miembroId={} nombre={} fechaVencimiento={} diasRestantes={}",
                    miembro.getId(), miembro.getNombre(), membresia.getFechaVencimiento(), diasRestantes);
        }

        log.info("Proceso de vencimientos completado: {} eventos publicados (ventana={} días)",
                publicados, dias);
        return publicados;
    }
}