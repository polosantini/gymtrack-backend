package com.gymtrack.pagos.infrastructure.messaging;

import com.gymtrack.common.events.MembresiaProximaVencer;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembresiaProximaVencerListener — crea VENCIMIENTO_PROXIMO")
class MembresiaProximaVencerListenerTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock private com.gymtrack.pagos.domain.repository.EventoNotificacionProcesadoRepository procesados;

    @InjectMocks
    private MembresiaProximaVencerListener listener;

    @Test
    @DisplayName("Crea notificación VENCIMIENTO_PROXIMO con mensaje y leida=false")
    void creaNotificacionCorrectamente() {
        LocalDate vencimiento = LocalDate.now().plusDays(3);
        MembresiaProximaVencer evento = new MembresiaProximaVencer(
                4L, "Diego", vencimiento, 3);
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        listener.consumir(evento);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());

        Notificacion n = captor.getValue();
        assertThat(n.getMiembroId()).isEqualTo(4L);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.VENCIMIENTO_PROXIMO);
        assertThat(n.isLeida()).isFalse();
        assertThat(n.getMensaje())
                .contains("3")
                .contains(vencimiento.toString());
    }

    @Test
    @DisplayName("Evento con miembroId null: no hace nada")
    void eventoInvalidoNoHaceNada() {
        MembresiaProximaVencer invalido = new MembresiaProximaVencer(
                null, "Diego", LocalDate.now().plusDays(3), 3);

        listener.consumir(invalido);

        verify(notificacionRepository, never()).save(any());
    }
}