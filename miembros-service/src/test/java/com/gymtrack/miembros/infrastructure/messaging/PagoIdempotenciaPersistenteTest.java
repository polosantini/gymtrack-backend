package com.gymtrack.miembros.infrastructure.messaging;
import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.miembros.domain.model.*;
import com.gymtrack.miembros.domain.repository.*;
import com.gymtrack.miembros.application.MiembroService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.*;import java.math.BigDecimal;import java.util.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:pagos_dedupe;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","spring.rabbitmq.listener.simple.auto-startup=false"})
class PagoIdempotenciaPersistenteTest {
 @org.springframework.boot.test.mock.mockito.MockBean org.springframework.amqp.core.AmqpAdmin amqpAdmin;
 @Autowired org.springframework.transaction.PlatformTransactionManager manager;
 @Autowired PagoRegistradoListener listener;@Autowired MiembroRepository miembros;@Autowired PagoAplicadoRepository pagos;
 @Autowired MiembroService service;
 private Long id;private final LocalDate base=LocalDate.of(2030,1,15);
 @BeforeEach void preparar(){pagos.deleteAll();miembros.deleteAll();var mem=new Membresia();mem.setPlan(TipoPlanMembresia.BASICO);mem.setEstado(EstadoMembresia.ACTIVA);mem.setFechaInicio(LocalDate.of(2026,1,1));mem.setFechaVencimiento(base);var m=new Miembro();m.setNombre("Prueba");m.setEmail("persistente@test.com");m.setMembresia(mem);id=miembros.save(m).getId();}
 private PagoRegistrado evento(long pago,String estado,String plan){return new PagoRegistrado(pago,id,plan,"MENSUAL",new BigDecimal("50000"),estado);}
 private LocalDate vence(){return service.obtenerPorId(id).membresia().fechaVencimiento();}
 @Test void secuenciaABANoExtiendeTerceraVezNiReviertePlan(){listener.consumir(evento(10,"REGISTRADO","BASICO"));listener.consumir(evento(11,"REGISTRADO","PREMIUM"));listener.consumir(evento(10,"REGISTRADO","BASICO"));assertEquals(base.plusMonths(2),vence());assertEquals("PREMIUM",service.obtenerPorId(id).membresia().plan());assertEquals(2,pagos.count());}
 @Test void pagoFallidoOPendienteNoActivaNiRenueva(){listener.consumir(evento(12,"FALLIDO","PREMIUM"));listener.consumir(evento(13,"PENDIENTE","PREMIUM"));assertEquals(base,vence());assertEquals(0,pagos.count());}
 @Test void entregasConcurrentesDelMismoPagoAcreditanUnaVez() throws Exception{var pool=Executors.newFixedThreadPool(2);var inicio=new CyclicBarrier(2);try{var a=pool.submit(()->{inicio.await();listener.consumir(evento(14,"REGISTRADO","FULL"));return null;});var b=pool.submit(()->{inicio.await();listener.consumir(evento(14,"REGISTRADO","FULL"));return null;});a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);assertEquals(base.plusMonths(1),vence());assertEquals(1,pagos.count());}finally{pool.shutdownNow();}}
 @Test void dtoVencidoCoincideConAcceso(){new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(t->{var m=miembros.findById(id).orElseThrow();m.getMembresia().setFechaVencimiento(LocalDate.of(2020,1,1));});assertEquals("VENCIDA",service.obtenerPorId(id).membresia().estado());assertFalse(service.verificarAcceso(id).activa());}
 @Test void eventoAnteriorNoReviertePlanConEntregaFueraDeOrden(){listener.consumir(evento(21,"REGISTRADO","PREMIUM"));listener.consumir(evento(20,"REGISTRADO","BASICO"));assertEquals(base.plusMonths(2),vence());assertEquals("PREMIUM",service.obtenerPorId(id).membresia().plan());}
 @Test void eventoDeAntesDeLaMigracionQuedaParaRevisionSinRenovar(){new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(t->{var m=miembros.findById(id).orElseThrow();m.getMembresia().setUltimoPagoId(50L);});assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class,()->listener.consumir(evento(40,"REGISTRADO","FULL")));assertEquals(base,vence());assertEquals(0,pagos.count());}
}
