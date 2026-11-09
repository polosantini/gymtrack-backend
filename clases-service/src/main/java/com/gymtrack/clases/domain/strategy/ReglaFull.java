package com.gymtrack.clases.domain.strategy;

import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class ReglaFull implements ReglasReservaStrategy {

    private final ReservaRepository reservaRepository;

    public ReglaFull(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Override
    public String plan() {
        return "FULL";
    }

    @Override
    public void validar(Long miembroId, LocalDate fecha) {
        // Se usa la fecha reservada, nunca la semana/día del reloj del servidor.
        LocalDate inicio = fecha;
        LocalDate fin = fecha;
        long existentes = reservaRepository.countByMiembroIdAndFechaBetweenAndEstadoIn(
                miembroId, inicio, fin, Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_ESPERA));
        if (existentes >= 1) {
            throw new ReglaReservaIncumplidaException(plan(), 1, "día");
        }
    }
}
