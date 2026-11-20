package com.gymtrack.pagos.domain.model;
import jakarta.persistence.*;
@Entity @Table(name="evento_notificacion_procesado")
public class EventoNotificacionProcesado {
 @Id @Column(length=250) private String clave;
 protected EventoNotificacionProcesado() {}
 public EventoNotificacionProcesado(String clave) {this.clave=clave;}
}
