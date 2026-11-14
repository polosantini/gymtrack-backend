package com.gymtrack.clases.domain.model;

import com.gymtrack.common.events.ReservaCupoAsignado;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Recibo persistente por cancelación y copia del aviso pendiente, en la misma transacción de asignación. */
@Entity
@Table(name = "cancelacion_procesada")
public class CancelacionProcesada {
    @Id
    private Long reservaId;
    private Long miembroId;
    private String claseNombre;
    private LocalDate fecha;
    private LocalTime hora;
    @Column(nullable = false)
    private boolean enviado;

    protected CancelacionProcesada() { }

    public CancelacionProcesada(Long reservaId, ReservaCupoAsignado evento) {
        this.reservaId = reservaId;
        this.enviado = evento == null;
        if (evento != null) {
            this.miembroId = evento.miembroId(); this.claseNombre = evento.claseNombre();
            this.fecha = evento.fecha(); this.hora = evento.hora();
        }
    }
    public Long getReservaId() { return reservaId; }
    public boolean isEnviado() { return enviado; }
    public void marcarEnviado() { enviado = true; }
    public ReservaCupoAsignado eventoPendiente() {
        return enviado ? null : new ReservaCupoAsignado(miembroId, claseNombre, fecha, hora);
    }
}
