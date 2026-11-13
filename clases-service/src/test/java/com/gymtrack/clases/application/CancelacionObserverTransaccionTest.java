package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.*;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.common.events.ReservaCancelada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:observer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class CancelacionObserverTransaccionTest {
    @Autowired private ReservaService servicio;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @Autowired private PlatformTransactionManager manager;
    @MockBean private RabbitTemplate rabbit;
    private Clase clase;
    private final LocalDate fecha = LocalDate.of(2026,11,17);

    @BeforeEach
    void preparar() {
        reservas.deleteAll(); clases.deleteAll();
        clase = new Clase(); clase.setNombre("Spinning"); clase.setInstructor("Instructor");
        clase.setDias(Set.of(DiaSemana.MARTES)); clase.setHora(LocalTime.of(18,0));
        clase.setDuracionMinutos(45); clase.setAforo(3); clase.setSala("Sala 1");
        clase = clases.save(clase);
    }
    private Reserva reserva(EstadoReserva estado, Integer posicion) {
        var r = new Reserva(); r.setMiembroId(4L); r.setClase(clase); r.setFecha(fecha);
        r.setEstado(estado); r.setPosicionEspera(posicion);
        return reservas.save(r);
    }
    @Test
    void commitPublicaUnaVezYLaBaseYaEstaConfirmada() {
        var r = reserva(EstadoReserva.CONFIRMADA, null);
        AtomicReference<EstadoReserva> estadoAlPublicar = new AtomicReference<>();
        doAnswer(inv -> {
            TransactionTemplate lectura = new TransactionTemplate(manager);
            lectura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            estadoAlPublicar.set(lectura.execute(s -> reservas.findById(r.getId()).orElseThrow().getEstado()));
            return null;
        }).when(rabbit).convertAndSend(eq("gymtrack.events"),eq("reserva.cancelada"),any(ReservaCancelada.class));
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            assertEquals("CANCELADA", servicio.cancelar(r.getId()).estado());
            verifyNoInteractions(rabbit); // El servicio terminó, pero la transacción externa aún no.
        });
        verify(rabbit).convertAndSend("gymtrack.events","reserva.cancelada",
                new ReservaCancelada(r.getId(),clase.getId(),fecha,true));
        assertEquals(EstadoReserva.CANCELADA, estadoAlPublicar.get());
    }
    @Test
    void rollbackNoPublicaYNoCancela() {
        var r = reserva(EstadoReserva.CONFIRMADA, null);
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            servicio.cancelar(r.getId()); verifyNoInteractions(rabbit); s.setRollbackOnly();
        });
        verifyNoInteractions(rabbit);
        assertEquals(EstadoReserva.CONFIRMADA,reservas.findById(r.getId()).orElseThrow().getEstado());
    }
    @Test
    void cancelarEnEsperaPublicaFalseSinAsignarNiRenumerar() {
        var r = reserva(EstadoReserva.EN_ESPERA, 1);
        var siguiente = reserva(EstadoReserva.EN_ESPERA, 2);
        servicio.cancelar(r.getId());
        verify(rabbit).convertAndSend("gymtrack.events","reserva.cancelada",
                new ReservaCancelada(r.getId(),clase.getId(),fecha,false));
        var sinCambios=reservas.findById(siguiente.getId()).orElseThrow();
        assertEquals(EstadoReserva.EN_ESPERA,sinCambios.getEstado()); assertEquals(2,sinCambios.getPosicionEspera());
    }
    @Test
    void segundaCancelacionEs409YNoPublicaOtroEvento() {
        var r = reserva(EstadoReserva.CONFIRMADA,null); servicio.cancelar(r.getId());
        assertThrows(EstadoReservaInvalidoException.class,()->servicio.cancelar(r.getId()));
        verify(rabbit,times(1)).convertAndSend(eq("gymtrack.events"),eq("reserva.cancelada"),any(ReservaCancelada.class));
    }
    @Test
    void reservaInexistenteNoPublica() {
        assertThrows(ReservaNotFoundException.class,()->servicio.cancelar(999999L));
        verifyNoInteractions(rabbit);
    }
}
