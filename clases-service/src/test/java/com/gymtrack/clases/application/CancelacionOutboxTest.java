package com.gymtrack.clases.application;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.clases.infrastructure.messaging.BandejaEventos;
import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.AmqpException;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:outbox_regresion;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false"})
class CancelacionOutboxTest {
 @Autowired TestRestTemplate http;@Autowired ReservaService servicio;@Autowired ClaseRepository clases;
 @Autowired ReservaRepository reservas;@Autowired EventoPendienteRepository eventos;@Autowired BandejaEventos bandeja;
 @Autowired PlatformTransactionManager manager;@MockBean RabbitTemplate rabbit;
 @BeforeEach void limpiar(){eventos.deleteAll();reservas.deleteAll();clases.deleteAll();ReflectionTestUtils.setField(bandeja,"confirmar",false);http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());}
 @AfterEach void restaurar(){ReflectionTestUtils.setField(bandeja,"confirmar",false);}
 private Reserva crear(){var c=new Clase();c.setNombre("Outbox");c.setInstructor("Prueba");c.setDias(EnumSet.allOf(DiaSemana.class));c.setHora(LocalTime.of(18,0));c.setDuracionMinutos(45);c.setAforo(2);c.setSala("Sala");c=clases.save(c);var r=new Reserva();r.setClase(c);r.setFecha(LocalDate.of(2026,11,24));r.setMiembroId(1L);r.setEstado(EstadoReserva.CONFIRMADA);return reservas.save(r);}
 @Test void rabbitCaidoConservaCancelacionYEventoYReintentaSinVolverACancelar(){
  var r=crear();doThrow(new AmqpException("Caido")).doNothing().when(rabbit).convertAndSend(anyString(),eq("reserva.cancelada"),any(ReservaCancelada.class));
  var respuesta=http.exchange("/api/reservas/"+r.getId()+"/cancelar",HttpMethod.PATCH,HttpEntity.EMPTY,String.class);
  assertEquals(200,respuesta.getStatusCode().value());assertEquals(EstadoReserva.CANCELADA,reservas.findById(r.getId()).orElseThrow().getEstado());
  String clave="reserva.cancelada:"+r.getId();assertFalse(eventos.findById(clave).orElseThrow().isEnviado());
  assertTrue(bandeja.intentarPublicar(clave));assertTrue(eventos.findById(clave).orElseThrow().isEnviado());
  assertTrue(bandeja.intentarPublicar(clave));verify(rabbit,times(2)).convertAndSend(anyString(),eq("reserva.cancelada"),any(ReservaCancelada.class));
 }
 @Test void rollbackNoGuardaEventoNiPublica(){var r=crear();new TransactionTemplate(manager).executeWithoutResult(s->{servicio.cancelar(r.getId());s.setRollbackOnly();});assertEquals(0,eventos.count());assertEquals(EstadoReserva.CONFIRMADA,reservas.findById(r.getId()).orElseThrow().getEstado());verifyNoInteractions(rabbit);}
 @Test void nackNoMarcaEnviadoPeroAckPosteriorSi(){
  var r=crear();var e=new ReservaCancelada(r.getId(),r.getClase().getId(),r.getFecha(),true);String clave="reserva.cancelada:"+r.getId();
  new TransactionTemplate(manager).executeWithoutResult(s->bandeja.registrar(clave,"reserva.cancelada",e));ReflectionTestUtils.setField(bandeja,"confirmar",true);
  doAnswer(inv->{CorrelationData cd=inv.getArgument(4);cd.getFuture().complete(new CorrelationData.Confirm(false,"nack"));return null;}).when(rabbit).convertAndSend(anyString(),anyString(),any(Object.class),any(org.springframework.amqp.core.MessagePostProcessor.class),any(CorrelationData.class));
  assertFalse(bandeja.intentarPublicar(clave));assertFalse(eventos.findById(clave).orElseThrow().isEnviado());
  doAnswer(inv->{CorrelationData cd=inv.getArgument(4);cd.getFuture().complete(new CorrelationData.Confirm(true,null));return null;}).when(rabbit).convertAndSend(anyString(),anyString(),any(Object.class),any(org.springframework.amqp.core.MessagePostProcessor.class),any(CorrelationData.class));
  assertTrue(bandeja.intentarPublicar(clave));assertTrue(eventos.findById(clave).orElseThrow().isEnviado());
 }
}
