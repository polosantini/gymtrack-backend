package com.gymtrack.pagos.infrastructure.messaging;
import com.gymtrack.pagos.domain.repository.*;
import com.gymtrack.pagos.application.NotificacionService;
import com.gymtrack.common.events.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.core.*;
import java.time.*;import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:notificaciones_dedupe;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","spring.rabbitmq.listener.simple.auto-startup=false"})
class NotificacionesIdempotenciaTest {
 @Autowired PagoRegistradoNotificacionListener pagos;@Autowired MembresiaProximaVencerListener vencimientos;
 @Autowired ReservaCupoAsignadoListener cupos;@Autowired NotificacionRepository notificaciones;
 @Autowired EventoNotificacionProcesadoRepository procesadas;@Autowired NotificacionService servicio;
 @MockBean AmqpAdmin admin;
 @BeforeEach void limpiar(){procesadas.deleteAll();notificaciones.deleteAll();}
 @Test void cadaTipoEsIdempotenteYSePuedeMarcarLeida(){
  var pago=new PagoRegistrado(1L,1L,"FULL","MENSUAL",new BigDecimal("80000"),"REGISTRADO");pagos.consumir(pago);pagos.consumir(pago);
  var vencimiento=new MembresiaProximaVencer(1L,"Prueba",LocalDate.of(2026,11,24),3);vencimientos.consumir(vencimiento);vencimientos.consumir(vencimiento);
  var cupo=new ReservaCupoAsignado(1L,"Yoga",LocalDate.of(2026,11,24),LocalTime.of(18,0));var props=new MessageProperties();props.setMessageId("reserva.cupo.asignado:7");var mensaje=new Message(new byte[0],props);cupos.consumir(cupo,mensaje);cupos.consumir(cupo,mensaje);
  assertEquals(3,notificaciones.count());assertEquals(3,procesadas.count());
  Long id=notificaciones.findAll().get(0).getId();assertTrue(servicio.marcarLeida(id).leida());assertTrue(servicio.marcarLeida(id).leida());
 }
 @Test void cupoSinIdentidadSeRechaza(){var e=new ReservaCupoAsignado(1L,"Yoga",LocalDate.of(2026,11,24),LocalTime.of(18,0));assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class,()->cupos.consumir(e,new Message(new byte[0],new MessageProperties())));assertEquals(0,notificaciones.count());}
}
