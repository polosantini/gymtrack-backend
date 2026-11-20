package com.gymtrack.pagos.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PagoRequest(
        @NotNull(message= "El miembroId es obligatorio") @Positive(message="El miembroId debe ser positivo") Long miembroId,
        @NotBlank(message = "El plan es obligatorio") String plan,
        @NotBlank(message = "El periodo es obligatorio") String periodo,
        @NotNull(message = "El monto es obligatorio") @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer=8,fraction=2,message="El monto admite hasta 8 enteros y 2 decimales") BigDecimal monto,
        @NotBlank(message = "El metodo de pago es obligatorio") String metodo
        ) {}
