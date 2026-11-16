package com.gymtrack.miembros.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtrack.common.events.EventosConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String COLA_PAGO_REGISTRADO = "miembros.pago.registrado.queue";
    public static final String COLA_MEMBRESIA_PROXIMA_VENCER = "notificaciones.membresia.proxima.vencer.queue";

    @Bean
    public TopicExchange gymtrackEventsExchange() {
        return new TopicExchange(EventosConfig.EXCHANGE, true, false);
    }

    @Bean
    public Queue pagoRegistradoQueue() {
        return QueueBuilder.durable(COLA_PAGO_REGISTRADO).build();
    }

    @Bean
    public Binding pagoRegistradoBinding(Queue pagoRegistradoQueue, TopicExchange gymtrackEventsExchange) {
        return BindingBuilder
                .bind(pagoRegistradoQueue)
                .to(gymtrackEventsExchange)
                .with(EventosConfig.RK_PAGO_REGISTRADO);
    }

    @Bean
    public Queue membresiaProximaVencerQueue() {
        return QueueBuilder.durable(COLA_MEMBRESIA_PROXIMA_VENCER).build();
    }

    @Bean
    public Binding membresiaProximaVencerBinding(Queue membresiaProximaVencerQueue,
                                                 TopicExchange gymtrackEventsExchange) {
        return BindingBuilder
                .bind(membresiaProximaVencerQueue)
                .to(gymtrackEventsExchange)
                .with(EventosConfig.RK_MEMBRESIA_PROXIMA_VENCER);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper.copy());
    }

    @Bean
    public ApplicationRunner declararColaAlArrancar(AmqpAdmin amqpAdmin) {
        return args -> {
            amqpAdmin.initialize();
        };
    }
}