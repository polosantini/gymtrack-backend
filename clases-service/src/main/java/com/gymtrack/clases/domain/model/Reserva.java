package com.gymtrack.clases.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Reserva de un miembro para una sesión concreta: una {@link Clase} en una fecha.
 *
 * <p>{@code miembroId} es una referencia lógica al servicio de Miembros
 * (no hay FK entre bases de datos de servicios distintos).</p>
 *
 * <p>{@code posicionEspera} solo tiene valor cuando el estado es EN_ESPERA;
 * en CONFIRMADA o CANCELADA puede ser null.</p>
 *
 * <p>{@code asistencia} es null mientras no se haya marcado si el miembro asistió.</p>
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long miembroId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column
    private Integer posicionEspera;

    /** true = asistió, false = no asistió, null = aún no se ha marcado. */
    @Column
    private Boolean asistencia;

    public Reserva() {
    }

    /**
     * Regla de la posición de espera: obligatoria (1, 2, 3...) cuando la reserva
     * está EN_ESPERA. Se revisa antes de guardar o actualizar.
     */
    @PrePersist
    @PreUpdate
    public void validarPosicionEspera() {
        if (estado == EstadoReserva.EN_ESPERA && (posicionEspera == null || posicionEspera < 1)) {
            throw new IllegalStateException("Una reserva EN_ESPERA debe tener posicionEspera mayor o igual a 1");
        }
    }

    // Getters
    public Long getId() { return id; }

    public Long getMiembroId() { return miembroId; }

    public Clase getClase() { return clase; }

    public LocalDate getFecha() { return fecha; }

    public EstadoReserva getEstado() { return estado; }

    public Integer getPosicionEspera() { return posicionEspera; }

    public Boolean getAsistencia() { return asistencia; }

    // Setters
    public void setMiembroId(Long miembroId) { this.miembroId = miembroId; }

    public void setClase(Clase clase) { this.clase = clase; }

    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public void setPosicionEspera(Integer posicionEspera) { this.posicionEspera = posicionEspera; }

    public void setAsistencia(Boolean asistencia) { this.asistencia = asistencia; }
}
