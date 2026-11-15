package com.gymtrack.miembros.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VencimientosJob {

    private static final Logger log = LoggerFactory.getLogger(VencimientosJob.class);

    private final VencimientoProximoService service;

    public VencimientosJob(VencimientoProximoService service) {
        this.service = service;
    }

    @Scheduled(
            cron = "${gymtrack.vencimientos.cron:0 0 8 * * *}",
            zone = "${gymtrack.vencimientos.zone:America/Bogota}"
    )
    public void ejecutar() {
        log.info("Iniciando tarea programada de vencimientos");
        try {
            int publicados = service.procesar(null);
            log.info("Tarea programada finalizada: {} eventos publicados", publicados);
        } catch (Exception ex) {
            log.error("Error en la tarea programada de vencimientos", ex);
        }
    }
}