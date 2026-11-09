package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.web.dto.ClaseRequest;
import com.gymtrack.clases.web.dto.ClaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClaseQueryServiceImpl implements ClaseQueryService {

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;

    public ClaseQueryServiceImpl(ClaseRepository claseRepository, ReservaRepository reservaRepository) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ClaseResponse> listar(LocalDate fecha) {
        LocalDate desde = (fecha != null) ? fecha : LocalDate.now(java.time.ZoneId.of("America/Bogota"));

        return claseRepository.findAll().stream()
                .filter(clase -> fecha == null || clase.seDictaEl(fecha))
                .map(clase -> toResponse(clase, clase.proximaSesionDesde(desde)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClaseResponse obtenerPorId(Long id) {
        Clase clase = claseRepository.findById(id).orElseThrow(() -> new ClaseNotFoundException(id));
        return toResponse(clase, clase.proximaSesionDesde(LocalDate.now(java.time.ZoneId.of("America/Bogota"))));
    }

    /** Cupos libres de una sesión = aforo - reservas CONFIRMADA de esa (clase, fecha). */
    private int cuposLibres(Clase clase, LocalDate fecha) {
        if (fecha == null) {
            return clase.getAforo();
        }
        long confirmadas = reservaRepository.countByClaseIdAndFechaAndEstado(
                clase.getId(), fecha, EstadoReserva.CONFIRMADA);
        return (int) Math.max(0, clase.getAforo() - confirmadas);
    }

    private ClaseResponse toResponse(Clase clase, LocalDate fecha) {
        return new ClaseResponse(
                clase.getId(),
                clase.getNombre(),
                clase.getInstructor(),
                clase.getDias(),
                clase.getHora(),
                clase.getDuracionMinutos(),
                clase.getAforo(),
                clase.getSala(),
                fecha,
                cuposLibres(clase, fecha)
        );
    }
    @Override
    @Transactional(readOnly = true)
    public ReservasClaseResponse listarPorClaseYFecha(Long claseId, LocalDate fecha) {
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new ClaseNotFoundException(claseId));

        if (!clase.seDictaEl(fecha)) {
            throw new FechaNoCorrespondeException(clase, fecha);
        }

        List<ReservaResponse> confirmados = reservaRepository
                .findByClaseIdAndFechaAndEstadoOrderByIdAsc(claseId, fecha, EstadoReserva.CONFIRMADA)
                .stream().map(this::toResponse).toList();

        // La espera se ordena por posición, nunca por id. Las CANCELADA no aparecen aquí.
        List<ReservaResponse> enEspera = reservaRepository
                .findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(claseId, fecha, EstadoReserva.EN_ESPERA)
                .stream().map(this::toResponse).toList();

        int cuposLibres = Math.max(0, clase.getAforo() - confirmados.size());

        return new ReservasClaseResponse(
                clase.getId(),
                clase.getNombre(),
                fecha,
                clase.getHora(),
                clase.getAforo(),
                cuposLibres,
                confirmados,
                enEspera
        );
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
