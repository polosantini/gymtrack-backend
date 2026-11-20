package com.gymtrack.clases.domain.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="evento_pendiente")
public class EventoPendiente {
 @Id @Column(length=180) private String id;
 @Column(nullable=false) private String routingKey;
 @Column(nullable=false,columnDefinition="text") private String payload;
 @Column(nullable=false) private boolean enviado;
 @Column(nullable=false) private LocalDateTime creado;
 protected EventoPendiente() {}
 public EventoPendiente(String id,String routingKey,String payload) {this.id=id;this.routingKey=routingKey;this.payload=payload;this.creado=LocalDateTime.now();}
 public String getId() {return id;}
 public String getRoutingKey() {return routingKey;}
 public String getPayload() {return payload;}
 public boolean isEnviado() {return enviado;}
 public void marcarEnviado() {enviado=true;}
}
