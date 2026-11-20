package com.gymtrack.miembros.infrastructure.messaging;
import org.springframework.context.annotation.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
@Configuration
public class RecuperacionEventosConfig {
 public static final String DLX="gymtrack.miembros.recuperacion";
 public static final String DLQ="gymtrack.miembros.recuperacion.queue";
 @Bean public TopicExchange recuperacionExchange() {return new TopicExchange(DLX,true,false);}
 @Bean public Queue recuperacionQueue() {return QueueBuilder.durable(DLQ).build();}
 @Bean public Binding recuperacionBinding() {return new Binding(DLQ,Binding.DestinationType.QUEUE,DLX,"#",null);}
 @Bean public MessageRecoverer messageRecoverer(RabbitTemplate rabbit) {
  var recuperador=new RepublishMessageRecovererWithConfirms(rabbit,DLX,"fallido",ConfirmType.CORRELATED);
  recuperador.setConfirmTimeout(5000);return recuperador;
 }
}
