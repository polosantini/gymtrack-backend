package com.gymtrack.pagos.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="pago")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long miembroId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanPago plan;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodoPago periodo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Pago() {}

    @PrePersist
    public void defaultCampos() {
        if (this.estado == null) {
            this.estado = EstadoPago.REGISTRADO;
        }
        if (this.fecha == null) {
            this.fecha = LocalDateTime.now();
        }
    }


    public Long getId() { return id; }

    public Long getMiembroId() {
        return miembroId;
    }

    public void setMiembroId(Long miembroId) {
        this.miembroId = miembroId;
    }

    public PlanPago getPlan() {
        return plan;
    }

    public void setPlan(PlanPago plan) {
        this.plan = plan;
    }

    public PeriodoPago getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoPago periodo) {
        this.periodo = periodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
