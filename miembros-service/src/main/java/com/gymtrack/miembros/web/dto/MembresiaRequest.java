package com.gymtrack.miembros.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MembresiaRequest(
        @NotBlank String plan
){}
