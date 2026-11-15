package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.EventosConfig;
import com.gymtrack.common.events.PagoRegistrado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoRegistradoPublisher — envía al exchange con la routing key correcta")
class PagoRegistradoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PagoRegistradoPublisher publisher;

    @Test
    void publicar_enviaAlExchangeYRoutingKeyDelContrato() {
        PagoRegistrado evento = new PagoRegistrado(
                5L, 2L, "FULL", "TRIMESTRAL", new BigDecimal("216000.00"), "REGISTRADO"
        );

        publisher.publicar(evento);

        verify(rabbitTemplate).convertAndSend(
                EventosConfig.EXCHANGE,
                EventosConfig.RK_PAGO_REGISTRADO,
                evento
        );
    }
}