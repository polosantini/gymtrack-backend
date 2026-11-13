package com.gymtrack.clases.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicadorReservaCanceladaTest {
    @Test
    void publicaEnExchangeYRoutingKeyExactos() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        var evento = new ReservaCancelada(9L, 1L, LocalDate.of(2026,11,17), false);
        new PublicadorReservaCancelada(rabbit).alCancelar(evento);
        verify(rabbit).convertAndSend("gymtrack.events", "reserva.cancelada", evento);
        verifyNoMoreInteractions(rabbit);
    }
    @Test
    void jsonDelContratoConFechaIsoYBoolean() {
        var config = new ReservasAmqpConfig();
        var converter = config.eventosJsonMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule()));
        var mensaje = converter.toMessage(new ReservaCancelada(9L,1L,LocalDate.of(2026,11,17),true), new MessageProperties());
        assertEquals("application/json", mensaje.getMessageProperties().getContentType());
        assertEquals("{\"reservaId\":9,\"claseId\":1,\"fecha\":\"2026-11-17\",\"liberaCupo\":true}",
                new String(mensaje.getBody(), StandardCharsets.UTF_8));
        assertEquals("gymtrack.events", config.gymtrackEventsExchange().getName());
        assertTrue(config.gymtrackEventsExchange().isDurable());
        assertEquals("topic", config.gymtrackEventsExchange().getType());
    }
}
