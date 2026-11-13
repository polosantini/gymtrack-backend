package com.gymtrack.clases.application;

import com.gymtrack.clases.application.port.PublicadorAsignacionPendiente;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.common.events.ReservaCancelada;
import com.gymtrack.common.events.ReservaCupoAsignado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.util.List;

@Service
public class AsignarCupoService {
    private final ClaseRepository clases;
    private final ReservaRepository reservas;
    private final CancelacionProcesadaRepository procesadas;
    private final PublicadorAsignacionPendiente publicador;

    public AsignarCupoService(ClaseRepository clases, ReservaRepository reservas,
                             CancelacionProcesadaRepository procesadas, PublicadorAsignacionPendiente publicador) {
        this.clases = clases; this.reservas = reservas; this.procesadas = procesadas; this.publicador = publicador;
    }

    @Transactional
    public void procesar(ReservaCancelada evento) {
        if (evento == null) throw new IllegalArgumentException("Evento de cancelación vacío");
        if (!evento.liberaCupo()) return;
        if (evento.reservaId() == null || evento.claseId() == null || evento.fecha() == null) {
            throw new IllegalArgumentException("Cancelación sin reservaId, claseId o fecha");
        }
        // Mismo lock que las reservas nuevas y cancelaciones: protege aforo, cola y deduplicación.
        Clase clase = clases.findByIdForUpdate(evento.claseId())
                .orElseThrow(() -> new IllegalArgumentException("Clase del evento inexistente"));
        if (procesadas.existsById(evento.reservaId())) {
            programarPublicacion(evento.reservaId()); // Solo reintenta el aviso si quedó pendiente.
            return;
        }
        Reserva cancelada = reservas.findById(evento.reservaId())
                .orElseThrow(() -> new IllegalArgumentException("Reserva del evento inexistente"));
        if (cancelada.getEstado() != EstadoReserva.CANCELADA
                || !cancelada.getClase().getId().equals(evento.claseId())
                || !cancelada.getFecha().equals(evento.fecha())) {
            throw new IllegalArgumentException("El evento no corresponde a una reserva cancelada de esa sesión");
        }
        long confirmadas = reservas.countByClaseIdAndFechaAndEstado(clase.getId(), evento.fecha(), EstadoReserva.CONFIRMADA);
        if (confirmadas >= clase.getAforo()) {
            procesadas.save(new CancelacionProcesada(evento.reservaId(), null));
            return;
        }
        List<Reserva> espera = reservas.findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(
                clase.getId(), evento.fecha(), EstadoReserva.EN_ESPERA);
        if (espera.isEmpty()) {
            // Persistir también este resultado: una repetición futura no debe asignar a una llegada nueva.
            procesadas.save(new CancelacionProcesada(evento.reservaId(), null));
            return;
        }
        Reserva primera = espera.get(0);
        primera.setEstado(EstadoReserva.CONFIRMADA); primera.setPosicionEspera(null);
        // El contrato permite ajustar posiciones al ASIGNAR un cupo, no al consultar o cancelar.
        for (int i = 1; i < espera.size(); i++) espera.get(i).setPosicionEspera(i);
        procesadas.save(new CancelacionProcesada(evento.reservaId(), new ReservaCupoAsignado(
                primera.getMiembroId(), clase.getNombre(), evento.fecha(), clase.getHora())));
        programarPublicacion(evento.reservaId());
    }

    private void programarPublicacion(Long reservaId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { publicador.publicar(reservaId); }
        });
    }
}
