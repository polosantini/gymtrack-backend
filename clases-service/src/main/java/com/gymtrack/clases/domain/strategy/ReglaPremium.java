package com.gymtrack.clases.domain.strategy;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReglaPremium implements ReglasReservaStrategy {
    @Override
    public String plan() {
        return "PREMIUM";
    }

    @Override
    public void validar(Long miembroId, LocalDate fecha) {
        // Sin límite de plan. Las validaciones de fecha, duplicado y aforo siguen vigentes.
    }
}
