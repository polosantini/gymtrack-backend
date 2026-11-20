package com.gymtrack.clases.infrastructure.messaging;
import com.gymtrack.clases.application.port.RegistroCancelaciones;
import com.gymtrack.common.events.*;
import org.springframework.stereotype.Component;
@Component
public class RegistroCancelacionesOutbox implements RegistroCancelaciones {
 private final BandejaEventos bandeja;
 public RegistroCancelacionesOutbox(BandejaEventos b) {bandeja=b;}
 @Override public void registrar(ReservaCancelada e) {bandeja.registrar("reserva.cancelada:"+e.reservaId(),EventosConfig.RK_RESERVA_CANCELADA,e);}
}
