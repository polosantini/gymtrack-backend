package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.common.rpc.*;
import com.gymtrack.miembros.domain.repository.MiembroRepository;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class ConsultaMembresiaPagoListener {
 private final MiembroRepository miembros;
 public ConsultaMembresiaPagoListener(MiembroRepository miembros) {this.miembros=miembros;}
 @RabbitListener(queues=MembresiaPagoRpc.QUEUE)
 @Transactional(readOnly=true)
 public RespuestaMembresiaPago consultar(ConsultarMembresiaPago consulta) {
  if(consulta==null || consulta.miembroId()==null || consulta.miembroId()<=0)
   throw new AmqpRejectAndDontRequeueException("Consulta de membresia sin miembro valido");
  var miembro=miembros.findById(consulta.miembroId());
  return new RespuestaMembresiaPago(consulta.miembroId(),miembro.isPresent(),
          miembro.isPresent() && miembro.get().getMembresia()!=null);
 }
}
