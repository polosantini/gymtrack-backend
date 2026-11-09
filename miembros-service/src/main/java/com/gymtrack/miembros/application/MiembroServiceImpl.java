package com.gymtrack.miembros.application;

import com.gymtrack.miembros.domain.exception.EmailDuplicadoException;
import com.gymtrack.miembros.domain.exception.MiembroNotFoundException;
import com.gymtrack.miembros.domain.exception.PlanInvalidoException;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import com.gymtrack.miembros.domain.model.Membresia;
import com.gymtrack.miembros.domain.model.Miembro;
import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import com.gymtrack.miembros.domain.plan.CreadorMembresia;
import com.gymtrack.miembros.domain.plan.PlanMembresia;
import com.gymtrack.miembros.domain.repository.MembresiaRepository;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import com.gymtrack.miembros.web.dto.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MiembroServiceImpl implements MiembroService {

    private final MiembroRepository miembroRepository;
    private final MembresiaRepository membresiaRepository;
    private final CreadorMembresia creadorMembresia;

    public MiembroServiceImpl(MiembroRepository miembroRepository, MembresiaRepository membresiaRepository, CreadorMembresia creadorMembresia) {
        this.miembroRepository = miembroRepository;
        this.membresiaRepository = membresiaRepository;
        this.creadorMembresia = creadorMembresia;
    }

    @Override
    public MiembroResponse registrar(MiembroRequest request) {
        if (miembroRepository.existsByEmail(request.email())) {
            throw new EmailDuplicadoException(request.email());
        }

        Miembro miembro = new Miembro();
        miembro.setNombre(request.nombre());
        miembro.setEmail(request.email());

        Miembro guardado = miembroRepository.save(miembro);

        return toResponse(guardado);
    }

    @Override
    public List<PlanResponse> listarPlanes() {
        return List.of(
                new PlanResponse(
                        "BASICO",
                        new BigDecimal("50000"),
                        new BigDecimal("135000"),
                        new BigDecimal("480000")
                ),
                new PlanResponse(
                        "FULL",
                        new BigDecimal("80000"),
                        new BigDecimal("216000"),
                        new BigDecimal("768000")
                ),
                new PlanResponse(
                        "PREMIUM",
                        new BigDecimal("120000"),
                        new BigDecimal("324000"),
                        new BigDecimal("1152000")
                )
        );
    }

    @Override
    public List<MiembroResponse> listar() {
        List<Miembro> miembros = miembroRepository.findAll();

        return miembros.stream().map(this::toResponse).toList();
    }

    @Override
    public MiembroResponse obtenerPorId(Long id) {
        Miembro miembro = miembroRepository.findById(id).orElseThrow(() -> new MiembroNotFoundException(id));
        return toResponse(miembro);
    }

    @Override
    public MembresiaResponse crearMembresia(Long miembroId, MembresiaRequest request) {
        Miembro miembro = miembroRepository.findById(miembroId)
                .orElseThrow(() -> new MiembroNotFoundException(miembroId));


        TipoPlanMembresia tipo = parsePlan(request.plan());
        PlanMembresia planComportamiento = creadorMembresia.crear(tipo);
        int dias = planComportamiento.diasVigencia();

        LocalDate fechaInicio = LocalDate.now();
        LocalDate fechaVencimiento = fechaInicio.plusDays(dias);

        if (miembro.getMembresia() != null) {
            Membresia membresiaAnterior = miembro.getMembresia();
            membresiaAnterior.setEstado(EstadoMembresia.VENCIDA);
            membresiaRepository.save(membresiaAnterior);
        }

        Membresia membresia = new Membresia();
        membresia.setPlan(tipo);
        membresia.setFechaInicio(fechaInicio);
        membresia.setFechaVencimiento(fechaVencimiento);

        Membresia guardada = membresiaRepository.save(membresia);
        miembro.setMembresia(guardada);
        miembroRepository.save(miembro);

        return toResponse(guardada);
    }

    @Override
    public AccesoResponse verificarAcceso(Long id) {
        Miembro miembro = miembroRepository.findById(id).orElseThrow(() -> new MiembroNotFoundException(id));
        Membresia membresia = miembro.getMembresia();

        if (membresia == null) {
            return new AccesoResponse(false, null);
        }
        boolean activa = membresia.getEstado() == EstadoMembresia.ACTIVA;
        String plan = membresia.getPlan().name();

        return new AccesoResponse(activa, plan);
    }

    private MembresiaResponse toResponse(Membresia m) {
        if (m == null) {
            return null;
        }

        return new MembresiaResponse(
                m.getId(),
                m.getPlan().name(),
                m.getEstado().name(),
                m.getFechaInicio(),
                m.getFechaVencimiento()
        );
    }

    private MiembroResponse toResponse(Miembro m) {
        return new MiembroResponse(
                m.getId(),
                m.getNombre(),
                m.getEmail(),
                m.getFechaRegistro(),
                toResponse(m.getMembresia())
        );
    }

    private TipoPlanMembresia parsePlan(String plan) {
        try {
            return TipoPlanMembresia.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PlanInvalidoException(plan);
        }
    }

}
