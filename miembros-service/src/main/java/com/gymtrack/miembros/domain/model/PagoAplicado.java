package com.gymtrack.miembros.domain.model;
import jakarta.persistence.*;
@Entity @Table(name="pago_aplicado")
public class PagoAplicado {
 @Id private Long pagoId;
 @Column(nullable=false) private Long miembroId;
 protected PagoAplicado() {}
 public PagoAplicado(Long pagoId,Long miembroId) {this.pagoId=pagoId;this.miembroId=miembroId;}
}
