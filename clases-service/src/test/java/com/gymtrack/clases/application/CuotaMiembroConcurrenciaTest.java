package com.gymtrack.clases.application;
import com.gymtrack.clases.application.port.*;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.web.dto.ReservaRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:cuota_miembro;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.datasource.hikari.maximum-pool-size=5","spring.sql.init.mode=never","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false"})
class CuotaMiembroConcurrenciaTest {
 @Autowired ReservaService servicio;@Autowired ClaseRepository clases;@Autowired ReservaRepository reservas;
 @MockBean MiembrosClient miembros;
 private final LocalDate fecha=LocalDate.of(2026,11,24);
 @BeforeEach void limpiar(){reservas.deleteAll();clases.deleteAll();}
 private Long clase(){var c=new Clase();c.setNombre("Cuota");c.setInstructor("Prueba");c.setDias(EnumSet.allOf(DiaSemana.class));c.setHora(LocalTime.of(18,0));c.setDuracionMinutos(45);c.setAforo(10);c.setSala("Sala");return clases.save(c).getId();}
 private void competir(String plan,int limite) throws Exception {
  when(miembros.consultarAcceso(77L)).thenReturn(new AccesoMembresia(true,plan));
  int n=10;var ids=new ArrayList<Long>();for(int i=0;i<n;i++)ids.add(clase());
  var pool=Executors.newFixedThreadPool(n);var inicio=new CyclicBarrier(n);
  try{var futuros=new ArrayList<Future<Boolean>>();for(Long id:ids)futuros.add(pool.submit(()->{inicio.await(10,TimeUnit.SECONDS);try{servicio.reservar(new ReservaRequest(77L,id,fecha));return true;}catch(ReglaReservaIncumplidaException ex){return false;}}));
   int aceptadas=0;for(var f:futuros)if(f.get(30,TimeUnit.SECONDS))aceptadas++;
   assertEquals(limite,aceptadas);assertEquals(limite,reservas.findByMiembroIdOrderByFechaDescIdDesc(77L).size());
  }finally{pool.shutdownNow();}
 }
 @RepeatedTest(3) void fullUnaAlDiaEnClasesDistintas() throws Exception{competir("FULL",1);}
 @RepeatedTest(3) void basicoDosPorSemanaEnClasesDistintas() throws Exception{competir("BASICO",2);}
 @Test void premiumConservaAccesoSinLimite() throws Exception{competir("PREMIUM",10);}
}
