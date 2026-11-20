package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.PagoRegistrado;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoRegistradoNotificacionListener — crea CONFIRMACION_PAGO, idempotente")
class PagoRegistradoNotificacionListenerTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock private com.gymtrack.pagos.domain.repository.EventoNotificacionProcesadoRepository procesados;

    @InjectMocks
    private PagoRegistradoNotificacionListener listener;

    private static final Long PAGO_ID = 42L;
    private static final Long MIEMBRO_ID = 3L;

    private PagoRegistrado evento() {
        return new PagoRegistrado(PAGO_ID, MIEMBRO_ID, "FULL", "MENSUAL",
                new BigDecimal("80000.00"), "REGISTRADO");
    }

    @Test
    @DisplayName("Crea notificación CONFIRMACION_PAGO con mensaje, referencia y leida=false")
    void creaNotificacionCorrectamente() {
        when(notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CONFIRMACION_PAGO, PAGO_ID)).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        listener.consumir(evento());

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());

        Notificacion n = captor.getValue();
        assertThat(n.getMiembroId()).isEqualTo(MIEMBRO_ID);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.CONFIRMACION_PAGO);
        assertThat(n.getReferenciaExterna()).isEqualTo(PAGO_ID);
        assertThat(n.isLeida()).isFalse();
        assertThat(n.getMensaje())
                .contains("FULL")
                .contains("MENSUAL")
                .contains("80000.00");
    }

    @Test
    @DisplayName("Idempotencia: si ya existe notificación con el mismo pagoId, no crea otra")
    void noDuplicaSiYaExiste() {
        when(notificacionRepository.existsByTipoAndReferenciaExterna(
                TipoNotificacion.CONFIRMACION_PAGO, PAGO_ID)).thenReturn(true);

        listener.consumir(evento());

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Evento con pagoId null: no hace nada")
    void eventoInvalidoNoHaceNada() {
        PagoRegistrado invalido = new PagoRegistrado(
                null, MIEMBRO_ID, "FULL", "MENSUAL", new BigDecimal("80000.00"), "REGISTRADO");

        listener.consumir(invalido);

        verify(notificacionRepository, never()).existsByTipoAndReferenciaExterna(any(), any());
        verify(notificacionRepository, never()).save(any());
    }
}