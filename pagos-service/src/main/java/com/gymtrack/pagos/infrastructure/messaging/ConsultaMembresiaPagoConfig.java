package com.gymtrack.pagos.infrastructure.messaging;
import com.gymtrack.common.rpc.MembresiaPagoRpc;
import org.springframework.context.annotation.*;
import org.springframework.amqp.core.*;
@Configuration
public class ConsultaMembresiaPagoConfig {
 @Bean public DirectExchange consultasMembresiaExchange() {return new DirectExchange(MembresiaPagoRpc.EXCHANGE,true,false);}
}
