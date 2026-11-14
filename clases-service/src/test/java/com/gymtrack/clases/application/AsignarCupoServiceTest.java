package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.application.port.MiembrosClient;
import com.gymtrack.clases.application.port.AccesoMembresia;
import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.common.events.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:asignacion;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class AsignarCupoServiceTest {
    @Autowired private AsignarCupoService servicio;
    @Autowired private ReservaService reservador;
    @MockBean private MiembrosClient miembros;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @Autowired private CancelacionProcesadaRepository recibos;
    @Autowired private PlatformTransactionManager manager;
    @MockBean private RabbitTemplate rabbit;
    private Clase clase;
    private Reserva cancelada;
    private final LocalDate fecha=LocalDate.of(2026,11,17);

    @BeforeEach void preparar() {
        clearInvocations(rabbit);
        recibos.deleteAll(); reservas.deleteAll(); clases.deleteAll();
        clase=clase(); cancelada=reserva(clase,1L,fecha,EstadoReserva.CANCELADA,null);
        reserva(clase,2L,fecha,EstadoReserva.CONFIRMADA,null);
        reserva(clase,3L,fecha,EstadoReserva.CONFIRMADA,null);
    }
    private Clase clase() {
        var c=new Clase(); c.setNombre("Spinning");c.setInstructor("Instructor");c.setDias(Set.of(DiaSemana.MARTES));
        c.setHora(LocalTime.of(18,0));c.setDuracionMinutos(45);c.setAforo(3);c.setSala("Sala 1");return clases.save(c);
    }
    private Reserva reserva(Clase c,long miembro,LocalDate dia,EstadoReserva estado,Integer posicion) {
        var r=new Reserva();r.setClase(c);r.setMiembroId(miembro);r.setFecha(dia);r.setEstado(estado);r.setPosicionEspera(posicion);
        return reservas.save(r);
    }
    private ReservaCancelada evento() {return new ReservaCancelada(cancelada.getId(),clase.getId(),fecha,true);}
    private Reserva recargar(Reserva r) {return reservas.findById(r.getId()).orElseThrow();}
    private long confirmadas() {return reservas.countByClaseIdAndFechaAndEstado(clase.getId(),fecha,EstadoReserva.CONFIRMADA);}

    @Test void falseNoAsignaNoGuardaReciboNiPublica() {
        var espera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        servicio.procesar(new ReservaCancelada(cancelada.getId(),clase.getId(),fecha,false));
        assertEquals(EstadoReserva.EN_ESPERA,recargar(espera).getEstado());assertEquals(0,recibos.count());verifyNoInteractions(rabbit);
    }
    @Test void asignaPorPosicionDeEsaSesionYNormalizaSoloAlPromover() {
        var otraFecha=reserva(clase,9L,fecha.plusDays(7),EstadoReserva.EN_ESPERA,1);
        var otraClase=reserva(clase(),10L,fecha,EstadoReserva.EN_ESPERA,1);
        var segunda=reserva(clase,6L,fecha,EstadoReserva.EN_ESPERA,5); // id menor, posición posterior
        var primera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,2);
        var tercera=reserva(clase,7L,fecha,EstadoReserva.EN_ESPERA,8);
        servicio.procesar(evento());
        assertEquals(EstadoReserva.CONFIRMADA,recargar(primera).getEstado());assertNull(recargar(primera).getPosicionEspera());
        assertEquals(1,recargar(segunda).getPosicionEspera());assertEquals(2,recargar(tercera).getPosicionEspera());
        assertEquals(EstadoReserva.EN_ESPERA,recargar(otraFecha).getEstado());assertEquals(EstadoReserva.EN_ESPERA,recargar(otraClase).getEstado());
        assertEquals(3,confirmadas());assertTrue(recibos.findById(cancelada.getId()).orElseThrow().isEnviado());
        verify(rabbit).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),
                eq(new ReservaCupoAsignado(4L,"Spinning",fecha,LocalTime.of(18,0))),any(MessagePostProcessor.class));
    }
    @Test void eventoRepetidoNoPromueveOtraPersonaNiRepiteAviso() {
        var primera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        var segunda=reserva(clase,6L,fecha,EstadoReserva.EN_ESPERA,2);
        servicio.procesar(evento());
        // Hay otro cupo libre después, pero repetir la cancelación original no debe asignarlo.
        var otra=reservas.findByClaseIdAndFechaAndEstadoOrderByIdAsc(clase.getId(),fecha,EstadoReserva.CONFIRMADA)
                .stream().filter(r->r.getMiembroId().equals(2L)).findFirst().orElseThrow();
        otra.setEstado(EstadoReserva.CANCELADA);reservas.save(otra);
        servicio.procesar(evento());
        assertEquals(2,confirmadas());
        assertEquals(EstadoReserva.CONFIRMADA,recargar(primera).getEstado());assertEquals(EstadoReserva.EN_ESPERA,recargar(segunda).getEstado());
        assertEquals(1,recibos.count());verify(rabbit,times(1)).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
    }
    @Test void colaVaciaNoPublicaNiAsignaALlegadaPosteriorPorUnDuplicado() {
        servicio.procesar(evento());var posterior=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);servicio.procesar(evento());
        assertEquals(EstadoReserva.EN_ESPERA,recargar(posterior).getEstado());assertEquals(1,recibos.count());verifyNoInteractions(rabbit);
    }
    @Test void claseLlenaNoSobrevendeAunqueLlegueEvento() {
        reserva(clase,8L,fecha,EstadoReserva.CONFIRMADA,null);var espera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        servicio.procesar(evento());assertEquals(3,confirmadas());assertEquals(EstadoReserva.EN_ESPERA,recargar(espera).getEstado());verifyNoInteractions(rabbit);
    }
    @Test void rollbackNoPromueveNoGuardaReciboNiPublica() {
        var espera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        new TransactionTemplate(manager).executeWithoutResult(s->{servicio.procesar(evento());verifyNoInteractions(rabbit);s.setRollbackOnly();});
        assertEquals(EstadoReserva.EN_ESPERA,recargar(espera).getEstado());assertEquals(0,recibos.count());verifyNoInteractions(rabbit);
    }
    @Test void avisoSeEnviaSoloTrasElCommitDeLaAsignacion() {
        var espera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        new TransactionTemplate(manager).executeWithoutResult(s->{servicio.procesar(evento());verifyNoInteractions(rabbit);});
        assertEquals(EstadoReserva.CONFIRMADA,recargar(espera).getEstado());
        verify(rabbit).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
    }
    @Test void falloDeEnvioConservaAvisoYRedeliverySoloReintentaLaPublicacion() {
        var primera=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);var segunda=reserva(clase,6L,fecha,EstadoReserva.EN_ESPERA,2);
        doThrow(new AmqpException("Fallo temporal")).doNothing().when(rabbit)
                .convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
        assertThrows(AmqpException.class,()->servicio.procesar(evento()));
        assertEquals(EstadoReserva.CONFIRMADA,recargar(primera).getEstado());assertFalse(recibos.findById(cancelada.getId()).orElseThrow().isEnviado());
        servicio.procesar(evento());assertTrue(recibos.findById(cancelada.getId()).orElseThrow().isEnviado());
        assertEquals(EstadoReserva.EN_ESPERA,recargar(segunda).getEstado());assertEquals(3,confirmadas());
        verify(rabbit,times(2)).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
    }
    @Test void eventoInconsistenteSeRechazaSinCambios() {
        reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        assertThrows(IllegalArgumentException.class,()->servicio.procesar(new ReservaCancelada(cancelada.getId(),clase.getId(),fecha.plusDays(7),true)));
        assertEquals(0,recibos.count());assertEquals(2,confirmadas());verifyNoInteractions(rabbit);
    }
    @RepeatedTest(3) void seisEntregasConcurrentesDelMismoEventoConfirmanSoloUna() throws Exception {
        reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);reserva(clase,6L,fecha,EstadoReserva.EN_ESPERA,2);
        CyclicBarrier barrera=new CyclicBarrier(6);ExecutorService pool=Executors.newFixedThreadPool(6);
        try {
            List<Future<?>> futuros=new ArrayList<>();for(int i=0;i<6;i++) futuros.add(pool.submit(()->{barrera.await(10,TimeUnit.SECONDS);servicio.procesar(evento());return null;}));
            for(var f:futuros) f.get(20,TimeUnit.SECONDS);
            assertEquals(3,confirmadas());assertEquals(1,reservas.countByClaseIdAndFechaAndEstado(clase.getId(),fecha,EstadoReserva.EN_ESPERA));
            assertEquals(1,recibos.count());verify(rabbit,times(1)).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
        } finally {pool.shutdownNow();}
    }

    @Test void asignacionCompiteConReservaNuevaSinSobreventaNiAdelantarLaCola() throws Exception {
        var primero=reserva(clase,4L,fecha,EstadoReserva.EN_ESPERA,1);
        when(miembros.consultarAcceso(100L)).thenReturn(new AccesoMembresia(true,"PREMIUM"));
        CyclicBarrier inicio=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            var asignacion=pool.submit(()->{inicio.await(10,TimeUnit.SECONDS);servicio.procesar(evento());return null;});
            var nueva=pool.submit(()->{inicio.await(10,TimeUnit.SECONDS);return reservador.reservar(new ReservaRequest(100L,clase.getId(),fecha));});
            asignacion.get(20,TimeUnit.SECONDS);var respuesta=nueva.get(20,TimeUnit.SECONDS);
            assertEquals(EstadoReserva.CONFIRMADA,recargar(primero).getEstado());assertEquals("EN_ESPERA",respuesta.estado());
            assertEquals(3,confirmadas());assertEquals(1,reservas.countByClaseIdAndFechaAndEstado(clase.getId(),fecha,EstadoReserva.EN_ESPERA));
            verify(rabbit,times(1)).convertAndSend(eq("gymtrack.events"),eq("reserva.cupo.asignado"),any(ReservaCupoAsignado.class),any(MessagePostProcessor.class));
        } finally {pool.shutdownNow();}
    }
}
