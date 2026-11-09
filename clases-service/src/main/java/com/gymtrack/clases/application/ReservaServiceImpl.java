package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;

    public ReservaServiceImpl(ClaseRepository claseRepository, ReservaRepository reservaRepository) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    public ReservaResponse reservar(ReservaRequest request) {
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

        // TODO Fase 2 (#25 del plan): consultar a miembros-service que el miembro exista y tenga membresía ACTIVA.
        // TODO Fase 2 (#23 del plan): aplicar la Strategy de reglas por plan (BASICO / FULL / PREMIUM) antes de reservar.
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

        Reserva guardada = reservaRepository.save(reserva);
        return toResponse(guardada);
    }

    private ReservaResponse toResponse(Reserva reserva) {
        Clase clase = reserva.getClase();
        return new ReservaResponse(
                reserva.getId(),
                reserva.getMiembroId(),
                clase.getId(),
                clase.getNombre(),
                reserva.getFecha(),
                clase.getHora(),
                reserva.getEstado().name(),
                reserva.getPosicionEspera()
        );
    }
}
