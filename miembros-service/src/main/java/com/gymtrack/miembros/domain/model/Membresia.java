package com.gymtrack.miembros.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="membresia")
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanMembresia plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMembresia estado;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    // Constructor
    public Membresia() {

    }


    @PrePersist
    public void suspendidaEstado() {
        if (this.estado == null) {
            this.estado = EstadoMembresia.SUSPENDIDA;
        }
    }


    // Getters
    public Long getId() {
        return id;
    }

    public PlanMembresia getPlan() {
        return plan;
    }

    public EstadoMembresia getEstado() {
        return estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    // Setters
    public void setPlan(PlanMembresia plan) {
        this.plan = plan;
    }

    public void setEstado(EstadoMembresia estado) {
        this.estado = estado;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}