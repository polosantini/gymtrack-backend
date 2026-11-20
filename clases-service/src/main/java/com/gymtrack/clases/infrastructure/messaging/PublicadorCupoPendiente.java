package com.gymtrack.clases.infrastructure.messaging;
import com.gymtrack.clases.application.port.PublicadorAsignacionPendiente;
import com.gymtrack.clases.domain.repository.CancelacionProcesadaRepository;
import com.gymtrack.common.events.EventosConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component
public class PublicadorCupoPendiente implements PublicadorAsignacionPendiente {
 private final CancelacionProcesadaRepository procesadas;
 private final BandejaEventos bandeja;
 public PublicadorCupoPendiente(CancelacionProcesadaRepository r,BandejaEventos b) {procesadas=r;bandeja=b;}
 @Override @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void publicar(Long id) {
  var recibo=procesadas.findByIdForUpdate(id).orElseThrow();var evento=recibo.eventoPendiente();if(evento==null)return;
  String clave="reserva.cupo.asignado:"+id;
  bandeja.registrar(clave,EventosConfig.RK_RESERVA_CUPO_ASIGNADO,evento);
  recibo.marcarEnviado(); // Transferido atomicamente a la outbox durable.
  bandeja.despuesCommit(clave);
 }
}
