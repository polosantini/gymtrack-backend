package com.gymtrack.miembros.web.dto;

import java.time.LocalDate;

public record MembresiaResponse(
        Long id,
        String plan,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento
) {}
