package com.gymtrack.clases.domain.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registro extensible: añadir un bean de estrategia no requiere editar este resolver. */
@Component
public class ReglasReservaResolver {

    private final Map<String, ReglasReservaStrategy> reglas;

    public ReglasReservaResolver(List<ReglasReservaStrategy> estrategias) {
        this.reglas = estrategias.stream().collect(Collectors.toUnmodifiableMap(
                ReglasReservaStrategy::plan, Function.identity()));
    }

    public ReglasReservaStrategy resolver(String plan) {
        // Nunca asignar Premium u otro plan por defecto si falta un plan válido.
        ReglasReservaStrategy regla = plan == null ? null : reglas.get(plan);
        if (regla == null) {
            throw new IllegalArgumentException("Plan de reserva no soportado: " + plan);
        }
        return regla;
    }
}
