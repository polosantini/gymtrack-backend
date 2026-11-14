package com.gymtrack.clases.infrastructure.messaging;

import com.gymtrack.clases.application.AsignarCupoService;
import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import java.time.LocalDate;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsumidorReservaCanceladaTest {
    @Test void delegaAlServicio() {
        var servicio=mock(AsignarCupoService.class);var evento=new ReservaCancelada(1L,1L,LocalDate.of(2026,11,17),true);
        new ConsumidorReservaCancelada(servicio).consumir(evento);verify(servicio).procesar(evento);
    }
    @Test void eventoInvalidoSeRechazaSinBucleDeReintentos() {
        var servicio=mock(AsignarCupoService.class);doThrow(new IllegalArgumentException("Inválido")).when(servicio).procesar(null);
        assertThrows(AmqpRejectAndDontRequeueException.class,()->new ConsumidorReservaCancelada(servicio).consumir(null));
    }
    @Test void colaPropiaDurableYBindingExacto() {
        var config=new ReservasAmqpConfig();assertEquals("gymtrack.clases.reserva.cancelada",config.cancelacionesClasesQueue().getName());
        assertTrue(config.cancelacionesClasesQueue().isDurable());assertEquals("reserva.cancelada",config.cancelacionesClasesBinding().getRoutingKey());
        assertEquals("gymtrack.events",config.cancelacionesClasesBinding().getExchange());
    }
}
