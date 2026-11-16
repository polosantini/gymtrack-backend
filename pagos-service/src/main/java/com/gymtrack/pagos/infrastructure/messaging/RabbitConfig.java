package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.EventosConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange gymtrackEventsExchange() {
        return new TopicExchange(EventosConfig.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ApplicationRunner declararExchangeAlArrancar(
            AmqpAdmin amqpAdmin,
            TopicExchange gymtrackEventsExchange) {
        return args -> {
            amqpAdmin.declareExchange(gymtrackEventsExchange);
        };
    }
}