package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.ReservaCupoAsignado;
import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import com.gymtrack.pagos.domain.repository.NotificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaCupoAsignadoListener — CUPO_LIBERADO con idempotencia por messageId")
class ReservaCupoAsignadoListenerTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock private com.gymtrack.pagos.domain.repository.EventoNotificacionProcesadoRepository procesados;

    @InjectMocks
    private ReservaCupoAsignadoListener listener;

    private static final Long MIEMBRO_ID = 2L;
    private static final Long RESERVA_CANCELADA_ID = 42L;

    private ReservaCupoAsignado evento() {
        return new ReservaCupoAsignado(
                MIEMBRO_ID,
                "Spinning",
                LocalDate.of(2026, 11, 17),
                LocalTime.of(18, 0)
        );
    }

    private Message mensajeConMessageId(String messageId) {
        MessageProperties props = new MessageProperties();
        props.setMessageId(messageId);
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("Crea CUPO_LIBERADO con referenciaExterna extraída del messageId")
    void creaNotificacionConReferencia() {
        when(notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CUPO_LIBERADO, RESERVA_CANCELADA_ID)).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        listener.consumir(evento(), mensajeConMessageId("reserva.cupo.asignado:" + RESERVA_CANCELADA_ID));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());

        Notificacion n = captor.getValue();
        assertThat(n.getMiembroId()).isEqualTo(MIEMBRO_ID);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.CUPO_LIBERADO);
        assertThat(n.getReferenciaExterna()).isEqualTo(RESERVA_CANCELADA_ID);
        assertThat(n.isLeida()).isFalse();
        assertThat(n.getMensaje())
                .contains("Spinning")
                .contains("2026-11-17")
                .contains("18:00");
    }

    @Test
    @DisplayName("Idempotencia: mismo messageId dos veces → no crea segunda notificación")
    void noDuplicaSiMismoMessageId() {
        when(notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CUPO_LIBERADO, RESERVA_CANCELADA_ID)).thenReturn(true);

        listener.consumir(evento(), mensajeConMessageId("reserva.cupo.asignado:" + RESERVA_CANCELADA_ID));

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void messageIdInvalidoSeRechazaSinCrearDuplicados() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class,
            ()->listener.consumir(evento(),mensajeConMessageId("formato-raro:123")));
        verify(notificacionRepository,never()).save(any());
    }
}
