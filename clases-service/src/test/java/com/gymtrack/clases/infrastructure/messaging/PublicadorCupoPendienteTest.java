package com.gymtrack.clases.infrastructure.messaging;

import com.gymtrack.clases.domain.model.CancelacionProcesada;
import com.gymtrack.clases.domain.repository.CancelacionProcesadaRepository;
import com.gymtrack.common.events.ReservaCupoAsignado;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicadorCupoPendienteTest {
    @Test void identidadEstableEnMetadatosSinCambiarElPayload() {
        var repo=mock(CancelacionProcesadaRepository.class);var rabbit=mock(RabbitTemplate.class);
        var evento=new ReservaCupoAsignado(4L,"Spinning",LocalDate.of(2026,11,17),LocalTime.of(18,0));
        var recibo=new CancelacionProcesada(1L,evento);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(recibo));
        new PublicadorCupoPendiente(repo,rabbit).publicar(1L);
        var captor=ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),eq(evento),captor.capture());
        var mensaje=captor.getValue().postProcessMessage(new Message(new byte[0],new MessageProperties()));
        assertEquals("reserva.cupo.asignado:1",mensaje.getMessageProperties().getMessageId());assertTrue(recibo.isEnviado());
    }
}
