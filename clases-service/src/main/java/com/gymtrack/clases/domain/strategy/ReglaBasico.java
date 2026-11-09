package com.gymtrack.clases.domain.strategy;

import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

@Component
public class ReglaBasico implements ReglasReservaStrategy {

    private final ReservaRepository reservaRepository;

    public ReglaBasico(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Override
    public String plan() {
        return "BASICO";
    }

    @Override
    public void validar(Long miembroId, LocalDate fecha) {
        // Se usa la fecha reservada, nunca la semana/día del reloj del servidor.
        LocalDate inicio = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fin = inicio.plusDays(6);
        long existentes = reservaRepository.countByMiembroIdAndFechaBetweenAndEstadoIn(
                miembroId, inicio, fin, Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_ESPERA));
        if (existentes >= 2) {
            throw new ReglaReservaIncumplidaException(plan(), 2, "semana");
        }
    }
}
