package com.gymtrack.clases.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtrack.common.events.EventosConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservasAmqpConfig {
    public static final String COLA_CANCELACIONES = "gymtrack.clases.reserva.cancelada";
    public static final String DLX = "gymtrack.clases.eventos.dlx";
    public static final String DLQ = COLA_CANCELACIONES + ".dlq";

    @Bean
    public Queue cancelacionesClasesQueue() {
        return QueueBuilder.durable(COLA_CANCELACIONES).deadLetterExchange(DLX).build();
    }
    @Bean
    public Binding cancelacionesClasesBinding() {
        return new Binding(COLA_CANCELACIONES, Binding.DestinationType.QUEUE,
                EventosConfig.EXCHANGE, EventosConfig.RK_RESERVA_CANCELADA, null);
    }
    @Bean public TopicExchange clasesEventosDlx() { return new TopicExchange(DLX, true, false); }
    @Bean public Queue cancelacionesClasesDlq() { return QueueBuilder.durable(DLQ).build(); }
    @Bean public Binding cancelacionesClasesDlqBinding() {
        return new Binding(DLQ, Binding.DestinationType.QUEUE, DLX, "#", null);
    }

    @Bean
    public TopicExchange gymtrackEventsExchange() {
        return new TopicExchange(EventosConfig.EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter eventosJsonMessageConverter(ObjectMapper mapper) {
        // Conserva JavaTimeModule y los formatos del evento de common. Boot lo conecta a RabbitTemplate.
        return new Jackson2JsonMessageConverter(mapper.copy());
    }
}
