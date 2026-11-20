package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.miembros.domain.model.EventoPendiente;
import com.gymtrack.miembros.domain.repository.EventoPendienteRepository;
import com.gymtrack.common.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.slf4j.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Outbox: el alta comparte transaccion con negocio; solo se marca enviado tras confirmacion del broker. */
@Service
public class BandejaEventos {
 private static final Logger log=LoggerFactory.getLogger(BandejaEventos.class);
 private static final Map<String,Class<?>> TIPOS=Map.of("membresia.proxima.vencer", MembresiaProximaVencer.class);
 private final EventoPendienteRepository repo;
 private final RabbitTemplate rabbit;
 private final ObjectMapper mapper;
 private final TransactionTemplate tx;
 @Value("${gymtrack.eventos.confirmar:true}") private boolean confirmar;
 @Value("${gymtrack.eventos.reintentos-auto:true}") private boolean automatico;
 public BandejaEventos(EventoPendienteRepository repo,RabbitTemplate rabbit,ObjectMapper mapper,PlatformTransactionManager manager) {
  this.repo=repo;this.rabbit=rabbit;this.mapper=mapper;this.tx=new TransactionTemplate(manager);
  tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void registrar(String id,String routing,Object evento) {
  if(!TIPOS.containsKey(routing))throw new IllegalArgumentException("Routing key no soportada");
  if(repo.existsById(id))return;
  try {repo.save(new EventoPendiente(id,routing,mapper.writeValueAsString(evento)));}
  catch(com.fasterxml.jackson.core.JsonProcessingException ex) {throw new IllegalArgumentException("Evento no serializable",ex);}
 }
 public void despuesCommit(String id) {
  if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Requiere transaccion");
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
   @Override public void afterCommit() {intentarPublicar(id);}
  });
 }
 public boolean intentarPublicar(String id) {
  try {
   tx.executeWithoutResult(s->{
    var e=repo.buscarConLock(id).orElseThrow();if(e.isEnviado())return;
    try {
     Object evento=mapper.readValue(e.getPayload(),TIPOS.get(e.getRoutingKey()));
     if(confirmar) {
      var correlacion=new CorrelationData(id);
      rabbit.convertAndSend(EventosConfig.EXCHANGE,e.getRoutingKey(),evento,m->{m.getMessageProperties().setMessageId(id);return m;},correlacion);
      var confirmacion=correlacion.getFuture().get(5,TimeUnit.SECONDS);
      if(!confirmacion.isAck() || correlacion.getReturned()!=null)throw new AmqpException("Evento no confirmado o sin cola destino");
     } else {
      // Solo en tests con RabbitTemplate simulado; produccion usa confirms y mandatory.
      if(e.getRoutingKey().equals("reserva.cupo.asignado"))
       rabbit.convertAndSend(EventosConfig.EXCHANGE,e.getRoutingKey(),evento,m->{m.getMessageProperties().setMessageId(id);return m;});
      else rabbit.convertAndSend(EventosConfig.EXCHANGE,e.getRoutingKey(),evento);
     }
     e.marcarEnviado();
    } catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new AmqpException("Publicacion interrumpida",ex);}
    catch(Exception ex) {throw new AmqpException("Publicacion pendiente",ex);}
   });
   return true;
  } catch(RuntimeException ex) {log.warn("Evento {} conservado para reintento: {}",id,ex.getMessage());return false;}
 }
 @Scheduled(initialDelayString="${gymtrack.eventos.reintento-ms:5000}",fixedDelayString="${gymtrack.eventos.reintento-ms:5000}")
 public void reintentarPendientes() {
  if(!automatico)return;
  for(var evento:repo.findFirst20ByEnviadoFalseOrderByCreadoAsc())intentarPublicar(evento.getId());
 }
}
