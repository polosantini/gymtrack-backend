package com.gymtrack.miembros.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record MiembroResponse(
        Long id,
        String nombre,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaRegistro,
        MembresiaResponse membresia
) {
}
