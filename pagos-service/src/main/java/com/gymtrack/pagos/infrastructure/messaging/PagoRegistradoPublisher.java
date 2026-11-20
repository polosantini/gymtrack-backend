package com.gymtrack.pagos.infrastructure.messaging;
import com.gymtrack.common.events.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class PagoRegistradoPublisher {
 private final BandejaEventos bandeja;
 public PagoRegistradoPublisher(BandejaEventos b) {bandeja=b;}
 @EventListener public void registrar(PagoRegistrado e) {bandeja.registrar("pago.registrado:"+e.pagoId(),EventosConfig.RK_PAGO_REGISTRADO,e);}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void publicar(PagoRegistrado e) {bandeja.intentarPublicar("pago.registrado:"+e.pagoId());}
}
