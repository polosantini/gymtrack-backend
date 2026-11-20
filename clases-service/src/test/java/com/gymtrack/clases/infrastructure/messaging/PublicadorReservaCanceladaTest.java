package com.gymtrack.clases.infrastructure.messaging;
import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.mockito.Mockito.*;
class PublicadorReservaCanceladaTest {
 @Test void publicaConIdentidadEstableTrasObserver() {
  var b=mock(BandejaEventos.class);var e=new ReservaCancelada(9L,1L,LocalDate.of(2026,11,17),true);
  new PublicadorReservaCancelada(b).alCancelar(e);verify(b).intentarPublicar("reserva.cancelada:9");
 }
 @Test void cancelacionEnEsperaTambienConservaIdentidad() {
  var b=mock(BandejaEventos.class);var e=new ReservaCancelada(9L,1L,LocalDate.of(2026,11,17),false);
  new PublicadorReservaCancelada(b).alCancelar(e);verify(b).intentarPublicar("reserva.cancelada:9");
 }
}
