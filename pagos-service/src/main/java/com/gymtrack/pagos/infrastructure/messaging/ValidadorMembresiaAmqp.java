package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.rpc.*;
import com.gymtrack.pagos.application.port.ValidadorMembresiaPago;
import com.gymtrack.pagos.domain.exception.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import java.util.Objects;

/** Una consulta AMQP acotada; no confia en flags del frontend ni en una replica local. */
@Component
public class ValidadorMembresiaAmqp implements ValidadorMembresiaPago {
    private final RabbitTemplate consultas;
    private final long timeoutMs;

    @Autowired
    public ValidadorMembresiaAmqp(ConnectionFactory connectionFactory, MessageConverter converter,
            @Value("${gymtrack.pagos.validacion-timeout-ms:2500}") long timeoutMs) {
        this(new RabbitTemplate(connectionFactory), timeoutMs);
        consultas.setMessageConverter(converter);
    }

    ValidadorMembresiaAmqp(RabbitTemplate consultas, long timeoutMs) {
        if (timeoutMs < 100 || timeoutMs > 5000) throw new IllegalArgumentException("Timeout RPC fuera de rango");
        this.consultas = consultas;
        this.timeoutMs = timeoutMs;
        consultas.setReplyTimeout(timeoutMs);
        consultas.setMandatory(true);
    }

    @Override
    public void validar(Long miembroId) {
        RespuestaMembresiaPago respuesta;
        try {
            respuesta = consultas.convertSendAndReceiveAsType(MembresiaPagoRpc.EXCHANGE,
                    MembresiaPagoRpc.ROUTING_KEY, new ConsultarMembresiaPago(miembroId), mensaje -> {
                        mensaje.getMessageProperties().setExpiration(Long.toString(timeoutMs));
                        return mensaje;
                    }, new ParameterizedTypeReference<RespuestaMembresiaPago>() {});
        } catch (AmqpException | ClassCastException ex) {
            throw new ValidacionPagoNoDisponibleException();
        }
        if (respuesta == null || !Objects.equals(miembroId, respuesta.miembroId())
                || respuesta.existeMiembro() == null || respuesta.tieneMembresia() == null
                || (!respuesta.existeMiembro() && respuesta.tieneMembresia())) {
            throw new ValidacionPagoNoDisponibleException();
        }
        if (!respuesta.existeMiembro()) throw new MiembroPagoNoEncontradoException();
        if (!respuesta.tieneMembresia()) throw new MiembroSinMembresiaException();
    }
    @jakarta.annotation.PreDestroy
    public void cerrar() { consultas.destroy(); }
}
