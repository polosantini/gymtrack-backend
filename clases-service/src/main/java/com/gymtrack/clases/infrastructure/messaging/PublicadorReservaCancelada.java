package com.gymtrack.clases.infrastructure.messaging;
import com.gymtrack.clases.domain.observer.ObservadorReservas;
import com.gymtrack.common.events.ReservaCancelada;
import org.springframework.stereotype.Component;
@Component
public class PublicadorReservaCancelada implements ObservadorReservas {
 private final BandejaEventos bandeja;
 public PublicadorReservaCancelada(BandejaEventos b) {bandeja=b;}
 @Override public void alCancelar(ReservaCancelada e) {bandeja.intentarPublicar("reserva.cancelada:"+e.reservaId());}
}
