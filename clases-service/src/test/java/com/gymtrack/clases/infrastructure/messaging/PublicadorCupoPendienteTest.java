package com.gymtrack.clases.infrastructure.messaging;
import com.gymtrack.clases.domain.model.CancelacionProcesada;
import com.gymtrack.clases.domain.repository.CancelacionProcesadaRepository;
import com.gymtrack.common.events.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PublicadorCupoPendienteTest {
 @Test void transfiereAvisoDurableSinCambiarPayload() {
  var repo=mock(CancelacionProcesadaRepository.class);var b=mock(BandejaEventos.class);
  var e=new ReservaCupoAsignado(4L,"Spinning",LocalDate.of(2026,11,17),LocalTime.of(18,0));
  var recibo=new CancelacionProcesada(1L,e);when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(recibo));
  new PublicadorCupoPendiente(repo,b).publicar(1L);
  verify(b).registrar("reserva.cupo.asignado:1",EventosConfig.RK_RESERVA_CUPO_ASIGNADO,e);
  verify(b).despuesCommit("reserva.cupo.asignado:1");assertTrue(recibo.isEnviado());
 }
}
