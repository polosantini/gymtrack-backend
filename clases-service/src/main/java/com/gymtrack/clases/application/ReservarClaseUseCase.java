package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.domain.strategy.ReglasReservaStrategy;
import com.gymtrack.clases.web.dto.ReservaRequest;

/**
 * Caso de uso independiente del plan y del cliente REST.
 * Se ejecuta dentro de la transacción de ReservaServiceImpl.
 * La factoría inyecta por constructor la estrategia elegida fuera de este caso de uso.
 */
public class ReservarClaseUseCase {
    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;
    private final ReglasReservaStrategy reglas;

    public ReservarClaseUseCase(ClaseRepository claseRepository, ReservaRepository reservaRepository,
                               ReglasReservaStrategy reglas) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
        this.reglas = reglas;
    }

    public Reserva ejecutar(ReservaRequest request) {
        Clase clase = claseRepository.findById(request.claseId())
                .orElseThrow(() -> new ClaseNotFoundException(request.claseId()));

        if (!clase.seDictaEl(request.fecha())) {
            throw new FechaNoCorrespondeException(clase, request.fecha());
        }

        // Una reserva CANCELADA no cuenta como duplicada: el miembro puede volver a reservar.
        boolean yaReservada = reservaRepository.existsByMiembroIdAndClaseIdAndFechaAndEstadoNot(
                request.miembroId(), clase.getId(), request.fecha(), EstadoReserva.CANCELADA);
        if (yaReservada) {
            throw new ReservaDuplicadaException(request.miembroId(), clase.getId(), request.fecha());
        }

        reglas.validar(request.miembroId(), request.fecha());
        // TODO Fase 2 (#27 del plan): bloquear la sesión (clase, fecha) con lock pesimista para que dos reservas
        //      simultáneas no superen el aforo. Hoy el conteo y el guardado no están protegidos contra concurrencia.

        long confirmadas = reservaRepository.countByClaseIdAndFechaAndEstado(
                clase.getId(), request.fecha(), EstadoReserva.CONFIRMADA);

        Reserva reserva = new Reserva();
        reserva.setMiembroId(request.miembroId());
        reserva.setClase(clase);
        reserva.setFecha(request.fecha());

        if (confirmadas < clase.getAforo()) {
            reserva.setEstado(EstadoReserva.CONFIRMADA);
        } else {
            long enEspera = reservaRepository.countByClaseIdAndFechaAndEstado(
                    clase.getId(), request.fecha(), EstadoReserva.EN_ESPERA);
            reserva.setEstado(EstadoReserva.EN_ESPERA);
            reserva.setPosicionEspera((int) enEspera + 1);
        }

        return reservaRepository.save(reserva);
    }
}
