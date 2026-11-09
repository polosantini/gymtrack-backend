package com.gymtrack.clases.application;

import com.gymtrack.clases.application.port.MiembrosClient;
import com.gymtrack.clases.domain.strategy.ReglasReservaResolver;
import com.gymtrack.clases.domain.strategy.ReglasReservaStrategy;
import com.gymtrack.clases.domain.exception.MembresiaInactivaException;
import com.gymtrack.clases.domain.exception.MiembrosNoDisponibleException;
import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.EstadoReservaInvalidoException;
import com.gymtrack.clases.domain.exception.ReservaNotFoundException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;

    private final MiembrosClient miembrosClient;
    private final ReglasReservaResolver reglasResolver;
    private final ReservarClaseUseCaseFactory casosReserva;

    public ReservaServiceImpl(ClaseRepository claseRepository, ReservaRepository reservaRepository,
                              MiembrosClient miembrosClient, ReglasReservaResolver reglasResolver,
                              ReservarClaseUseCaseFactory casosReserva) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
        this.miembrosClient = miembrosClient;
        this.reglasResolver = reglasResolver;
        this.casosReserva = casosReserva;
    }

    @Override
    public ReservaResponse reservar(ReservaRequest request) {
        var acceso = miembrosClient.consultarAcceso(request.miembroId());
        if (!Boolean.TRUE.equals(acceso.activa())) {
            throw new MembresiaInactivaException();
        }
        ReglasReservaStrategy regla;
        try {
            regla = reglasResolver.resolver(acceso.plan());
        } catch (IllegalArgumentException ex) {
            // Una respuesta incompleta de Miembros no debe conceder reservas sin regla.
            throw new MiembrosNoDisponibleException();
        }
        return toResponse(casosReserva.crear(regla).ejecutar(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorMiembro(Long miembroId) {
        return reservaRepository.findByMiembroIdOrderByFechaDescIdDesc(miembroId).stream()
                .map(this::toResponse)
                .toList();
    }


    @Override
    public ReservaResponse cancelar(Long reservaId) {
        Reserva reserva = buscarReserva(reservaId);

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new EstadoReservaInvalidoException("La reserva " + reservaId + " ya está cancelada");
        }

        EstadoReserva estadoAnterior = reserva.getEstado();
        Integer posicionAnterior = reserva.getPosicionEspera();

        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setPosicionEspera(null);

        // Si se va alguien de la lista de espera, los que estaban detrás suben un puesto
        // (así las posiciones siguen siendo 1, 2, 3... sin huecos ni repetidos).
        if (estadoAnterior == EstadoReserva.EN_ESPERA && posicionAnterior != null) {
            reservaRepository.findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(
                            reserva.getClase().getId(), reserva.getFecha(), EstadoReserva.EN_ESPERA)
                    .stream()
                    .filter(otra -> otra != reserva && otra.getPosicionEspera() > posicionAnterior)
                    .forEach(otra -> otra.setPosicionEspera(otra.getPosicionEspera() - 1));
        }

        boolean liberaCupo = estadoAnterior == EstadoReserva.CONFIRMADA;
        // TODO Fase 3 (Observer): publicar ReservaCancelada(reserva.getId(), claseId, fecha, liberaCupo)
        //      en el exchange gymtrack.events con la routing key EventosConfig.RK_RESERVA_CANCELADA.
        // TODO Fase 2 (lista de espera): si liberaCupo es true, pasar a CONFIRMADA al primero en espera.

        // No hace falta save(): la reserva está gestionada por JPA dentro de la transacción
        // y los cambios se guardan al terminar el método.
        return toResponse(reserva);
    }

    @Override
    public ReservaResponse marcarAsistencia(Long reservaId, boolean asistio) {
        Reserva reserva = buscarReserva(reservaId);

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new EstadoReservaInvalidoException("Solo se puede marcar asistencia en reservas CONFIRMADA. La reserva "
                    + reservaId + " está " + reserva.getEstado());
        }

        reserva.setAsistencia(asistio);
        return toResponse(reserva);
    }

    private Reserva buscarReserva(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNotFoundException(reservaId));
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
                reserva.getPosicionEspera(),
                reserva.getAsistencia()
        );
    }
}
