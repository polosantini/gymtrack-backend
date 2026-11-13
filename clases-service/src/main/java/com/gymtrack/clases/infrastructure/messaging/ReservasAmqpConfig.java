package com.gymtrack.clases.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtrack.common.events.EventosConfig;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservasAmqpConfig {
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
