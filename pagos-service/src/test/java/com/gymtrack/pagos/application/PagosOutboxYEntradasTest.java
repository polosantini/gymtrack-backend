package com.gymtrack.pagos.application;
import com.gymtrack.pagos.domain.repository.*;
import com.gymtrack.pagos.infrastructure.messaging.*;
import com.gymtrack.common.events.PagoRegistrado;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.AmqpException;
import org.springframework.http.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:pago_outbox;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","spring.rabbitmq.listener.simple.auto-startup=false"})
class PagosOutboxYEntradasTest {
 @Autowired TestRestTemplate http;@Autowired PagoRepository pagos;@Autowired EventoPendienteRepository pendientes;
 @Autowired BandejaEventos bandeja;@Autowired PagoService servicio;@Autowired PlatformTransactionManager manager;
 @MockBean com.gymtrack.pagos.application.port.ValidadorMembresiaPago validadorMembresia;
 @MockBean RabbitTemplate rabbit;@MockBean AmqpAdmin admin;
 @BeforeEach void limpiar(){clearInvocations(rabbit);pendientes.deleteAll();pagos.deleteAll();}
 private Map<String,Object> datos(){return Map.of("miembroId",1,"plan","FULL","periodo","MENSUAL","monto",80000,"metodo","EFECTIVO");}
 @Test void pagoYEventoPersistenAunqueRabbitFalleYSeRecuperaSinDuplicarPago(){
  doThrow(new AmqpException("Caido")).doNothing().when(rabbit).convertAndSend(anyString(),eq("pago.registrado"),any(PagoRegistrado.class));
  var r=http.postForEntity("/api/pagos",datos(),Map.class);assertEquals(201,r.getStatusCode().value());assertEquals("REGISTRADO",r.getBody().get("estado"));
  String clave="pago.registrado:"+r.getBody().get("id");assertEquals(1,pagos.count());assertFalse(pendientes.findById(clave).orElseThrow().isEnviado());
  assertTrue(bandeja.intentarPublicar(clave));assertTrue(pendientes.findById(clave).orElseThrow().isEnviado());assertEquals(1,pagos.count());
 }
 @Test void rollbackPagoTampocoDejaEvento(){
  new TransactionTemplate(manager).executeWithoutResult(s->{servicio.crearPago(new com.gymtrack.pagos.web.dto.PagoRequest(1L,"FULL","MENSUAL",new java.math.BigDecimal("80000"),"EFECTIVO"));s.setRollbackOnly();});
  assertEquals(0,pagos.count());assertEquals(0,pendientes.count());verifyNoInteractions(rabbit);
 }
 @Test void parametroIncorrectoEs400(){assertEquals(400,http.getForEntity("/api/pagos?miembroId=abc",String.class).getStatusCode().value());}
 @Test void jsonInvalidoEs400(){var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);assertEquals(400,http.postForEntity("/api/pagos",new HttpEntity<>("{\"miembroId\":",headers),String.class).getStatusCode().value());}
 @Test void montoFueraDePrecisionEs400(){var d=new java.util.HashMap<>(datos());d.put("monto",100000000);assertEquals(400,http.postForEntity("/api/pagos",d,String.class).getStatusCode().value());}
 @Test void sinMembresiaEs409YSinPagoNiEvento(){
  doThrow(new com.gymtrack.pagos.domain.exception.MiembroSinMembresiaException()).when(validadorMembresia).validar(1L);
  var respuesta=http.postForEntity("/api/pagos",datos(),Map.class);assertEquals(409,respuesta.getStatusCode().value());
  assertEquals("El miembro no tiene una membresía creada",respuesta.getBody().get("message"));
  assertEquals("/api/pagos",respuesta.getBody().get("path"));assertEquals(0,pagos.count());assertEquals(0,pendientes.count());verifyNoInteractions(rabbit);
 }
 @Test void miembroInexistenteEs404YSinPago(){doThrow(new com.gymtrack.pagos.domain.exception.MiembroPagoNoEncontradoException()).when(validadorMembresia).validar(1L);assertEquals(404,http.postForEntity("/api/pagos",datos(),Map.class).getStatusCode().value());assertEquals(0,pagos.count());assertEquals(0,pendientes.count());verifyNoInteractions(rabbit);}
 @Test void timeoutValidacionEs503YSinPago(){doThrow(new com.gymtrack.pagos.domain.exception.ValidacionPagoNoDisponibleException()).when(validadorMembresia).validar(1L);assertEquals(503,http.postForEntity("/api/pagos",datos(),Map.class).getStatusCode().value());assertEquals(0,pagos.count());assertEquals(0,pendientes.count());verifyNoInteractions(rabbit);}
}
