package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.domain.strategy.ReglasReservaStrategy;
import org.springframework.stereotype.Component;

/** Inyecta la estrategia de cada petición sin guardar el plan en un singleton mutable. */
@Component
public class ReservarClaseUseCaseFactory {
    private final ClaseRepository clases;
    private final ReservaRepository reservas;

    public ReservarClaseUseCaseFactory(ClaseRepository clases, ReservaRepository reservas) {
        this.clases = clases;
        this.reservas = reservas;
    }

    public ReservarClaseUseCase crear(ReglasReservaStrategy regla) {
        return new ReservarClaseUseCase(clases, reservas, regla);
    }
}
