package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.common.rpc.MembresiaPagoRpc;
import org.springframework.context.annotation.*;
import org.springframework.amqp.core.*;
@Configuration
public class ConsultaMembresiaPagoConfig {
 @Bean public DirectExchange consultasMembresiaExchange() {return new DirectExchange(MembresiaPagoRpc.EXCHANGE,true,false);}
 @Bean public Queue consultasMembresiaPagoQueue() {return QueueBuilder.durable(MembresiaPagoRpc.QUEUE).build();}
 @Bean public Binding consultasMembresiaPagoBinding() {return new Binding(MembresiaPagoRpc.QUEUE,Binding.DestinationType.QUEUE,MembresiaPagoRpc.EXCHANGE,MembresiaPagoRpc.ROUTING_KEY,null);}
}
