package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.web.dto.ClaseRequest;
import com.gymtrack.clases.web.dto.ClaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@Transactional
public class ClaseServiceImpl implements ClaseService {
    private final ClaseRepository claseRepository;
    public ClaseServiceImpl(ClaseRepository claseRepository) {
        this.claseRepository = claseRepository;
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

        return new ClaseResponse(guardada.getId(), guardada.getNombre(), guardada.getInstructor(),
                guardada.getDias(), guardada.getHora(), guardada.getDuracionMinutos(), guardada.getAforo(),
                guardada.getSala(), guardada.proximaSesionDesde(LocalDate.now(java.time.ZoneId.of("America/Bogota"))),
                guardada.getAforo());
    }
}
