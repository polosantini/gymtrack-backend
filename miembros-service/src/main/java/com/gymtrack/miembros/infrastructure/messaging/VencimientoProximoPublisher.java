package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.common.events.*;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
@Component
public class VencimientoProximoPublisher {
 private final BandejaEventos bandeja;
 public VencimientoProximoPublisher(BandejaEventos b) {bandeja=b;}
 public void publicar(MembresiaProximaVencer e) {
  String clave="membresia.proxima.vencer:"+e.miembroId()+":"+LocalDate.now()+":"+e.fechaVencimiento();
  bandeja.registrar(clave,EventosConfig.RK_MEMBRESIA_PROXIMA_VENCER,e);bandeja.despuesCommit(clave);
 }
}
