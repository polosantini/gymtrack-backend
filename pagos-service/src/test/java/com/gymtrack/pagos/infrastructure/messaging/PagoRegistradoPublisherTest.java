package com.gymtrack.pagos.infrastructure.messaging;
import com.gymtrack.common.events.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
class PagoRegistradoPublisherTest {
 @Test void registraElEventoYDespuesSolicitaPublicacion() {
  var b=mock(BandejaEventos.class);var p=new PagoRegistradoPublisher(b);
  var e=new PagoRegistrado(5L,2L,"FULL","TRIMESTRAL",new BigDecimal("216000"),"REGISTRADO");
  p.registrar(e);p.publicar(e);
  var orden=inOrder(b);orden.verify(b).registrar("pago.registrado:5",EventosConfig.RK_PAGO_REGISTRADO,e);
  orden.verify(b).intentarPublicar("pago.registrado:5");
 }
}
