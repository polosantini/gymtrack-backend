package com.gymtrack.pagos.infrastructure.messaging;

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

    public static final String COLA_NOTIFICACIONES_PAGO = "notificaciones.pago.registrado.queue";
    public static final String COLA_NOTIFICACIONES_VENCIMIENTO = "notificaciones.membresia.proxima.vencer.queue";
    public static final String COLA_NOTIFICACIONES_CUPO_ASIGNADO = "notificaciones.reserva.cupo.asignado.queue";

    @Bean
    public TopicExchange gymtrackEventsExchange() {
        return new TopicExchange(EventosConfig.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue notificacionesPagoRegistradoQueue() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES_PAGO).build();
    }

    @Bean
    public Binding notificacionesPagoRegistradoBinding(Queue notificacionesPagoRegistradoQueue,
                                                       TopicExchange gymtrackEventsExchange) {
        return BindingBuilder
                .bind(notificacionesPagoRegistradoQueue)
                .to(gymtrackEventsExchange)
                .with(EventosConfig.RK_PAGO_REGISTRADO);
    }

    @Bean
    public Queue notificacionesVencimientoQueue() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES_VENCIMIENTO).build();
    }

    @Bean
    public Binding notificacionesVencimientoBinding(Queue notificacionesVencimientoQueue,
                                                    TopicExchange gymtrackEventsExchange) {
        return BindingBuilder
                .bind(notificacionesVencimientoQueue)
                .to(gymtrackEventsExchange)
                .with(EventosConfig.RK_MEMBRESIA_PROXIMA_VENCER);
    }

    @Bean
    public Queue notificacionesCupoAsignadoQueue() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES_CUPO_ASIGNADO).build();
    }

    @Bean
    public Binding notificacionesCupoAsignadoBinding(Queue notificacionesCupoAsignadoQueue,
                                                     TopicExchange gymtrackEventsExchange) {
        return BindingBuilder
                .bind(notificacionesCupoAsignadoQueue)
                .to(gymtrackEventsExchange)
                .with(EventosConfig.RK_RESERVA_CUPO_ASIGNADO);
    }

    @Bean
    public ApplicationRunner declararExchangeYColasAlArrancar(AmqpAdmin amqpAdmin) {
        return args -> amqpAdmin.initialize();
    }
}