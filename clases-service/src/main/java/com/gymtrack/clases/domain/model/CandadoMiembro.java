package com.gymtrack.clases.domain.model;
import jakarta.persistence.*;
@Entity @Table(name="candado_miembro")
public class CandadoMiembro {
 @Id private Long miembroId;
 protected CandadoMiembro() {}
 public CandadoMiembro(Long id) {miembroId=id;}
}
