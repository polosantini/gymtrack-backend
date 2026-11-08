package com.gymtrack.miembros.web.dto;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;

public record MembresiaResponse(
        Long id,
        String plan,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento
) {}
