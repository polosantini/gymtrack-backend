package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
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
@Transactional
public class ClaseServiceImpl implements ClaseService {

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;

    public ClaseServiceImpl(ClaseRepository claseRepository, ReservaRepository reservaRepository) {
        this.claseRepository = claseRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    public ClaseResponse crear(ClaseRequest request) {
        Clase clase = new Clase();
        clase.setNombre(request.nombre());
        clase.setInstructor(request.instructor());
        clase.setDias(request.dias());
        clase.setHora(request.hora());
        clase.setDuracionMinutos(request.duracionMinutos());
        clase.setAforo(request.aforo());
        clase.setSala(request.sala());

        Clase guardada = claseRepository.save(clase);

        return toResponse(guardada, guardada.proximaSesionDesde(LocalDate.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaseResponse> listar(LocalDate fecha) {
        LocalDate desde = (fecha != null) ? fecha : LocalDate.now();

        return claseRepository.findAll().stream()
                .filter(clase -> fecha == null || clase.seDictaEl(fecha))
                .map(clase -> toResponse(clase, clase.proximaSesionDesde(desde)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClaseResponse obtenerPorId(Long id) {
        Clase clase = claseRepository.findById(id).orElseThrow(() -> new ClaseNotFoundException(id));
        return toResponse(clase, clase.proximaSesionDesde(LocalDate.now()));
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
}
