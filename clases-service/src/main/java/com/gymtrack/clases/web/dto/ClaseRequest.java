package com.gymtrack.clases.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymtrack.clases.domain.model.DiaSemana;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.Set;

public record ClaseRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 255) String nombre,
        @NotBlank(message = "El instructor es obligatorio") @Size(max = 255) String instructor,
        @NotEmpty(message = "Debe indicar al menos un día") Set<@NotNull(message = "Día inválido") DiaSemana> dias,
        @NotNull(message = "La hora es obligatoria") @JsonFormat(pattern = "HH:mm") LocalTime hora,
        @NotNull(message = "La duración es obligatoria") @Min(value = 1, message = "La duración debe ser mayor que cero") Integer duracionMinutos,
        @NotNull(message = "El aforo es obligatorio") @Min(value = 1, message = "El aforo debe ser mayor que cero") Integer aforo,
        @NotBlank(message = "La sala es obligatoria") @Size(max = 255) String sala
) {}
