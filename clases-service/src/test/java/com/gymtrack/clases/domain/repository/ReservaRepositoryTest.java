package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.DiaSemana;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@DisplayName("ClaseRepository y ReservaRepository")
class ReservaRepositoryTest {

    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);

    @Autowired
    private ClaseRepository claseRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    private Clase guardarClase() {
        Clase clase = new Clase();
        clase.setNombre("Spinning");
        clase.setInstructor("Carlos");
        clase.setDias(Set.of(DiaSemana.MARTES, DiaSemana.JUEVES));
        clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45);
        clase.setAforo(3);
        clase.setSala("Sala 1");
        return claseRepository.saveAndFlush(clase);
    }

    private Reserva reserva(Clase clase, Long miembroId, EstadoReserva estado, Integer posicion) {
        Reserva r = new Reserva();
        r.setClase(clase);
        r.setMiembroId(miembroId);
        r.setFecha(MARTES_17);
        r.setEstado(estado);
        r.setPosicionEspera(posicion);
        return r;
    }

    @Test
    @DisplayName("Guarda una clase y conserva sus días al leerla")
    void guardaClaseConDias() {
        Clase guardada = guardarClase();

        Clase leida = claseRepository.findById(guardada.getId()).orElseThrow();

        assertNotNull(leida.getId());
        assertEquals(EnumSet.of(DiaSemana.MARTES, DiaSemana.JUEVES), leida.getDias());
        assertEquals(LocalTime.of(18, 0), leida.getHora());
    }

    @Test
    @DisplayName("countByClaseIdAndFechaAndEstado cuenta solo las reservas de esa clase, fecha y estado")
    void cuentaReservasPorClaseFechaYEstado() {
        Clase clase = guardarClase();
        reservaRepository.save(reserva(clase, 1L, EstadoReserva.CONFIRMADA, null));
        reservaRepository.save(reserva(clase, 2L, EstadoReserva.CONFIRMADA, null));
        reservaRepository.save(reserva(clase, 3L, EstadoReserva.CANCELADA, null));
        reservaRepository.save(reserva(clase, 4L, EstadoReserva.EN_ESPERA, 1));

        assertEquals(2, reservaRepository.countByClaseIdAndFechaAndEstado(clase.getId(), MARTES_17, EstadoReserva.CONFIRMADA));
        assertEquals(1, reservaRepository.countByClaseIdAndFechaAndEstado(clase.getId(), MARTES_17, EstadoReserva.EN_ESPERA));
        assertEquals(0, reservaRepository.countByClaseIdAndFechaAndEstado(clase.getId(), MARTES_17.plusDays(2), EstadoReserva.CONFIRMADA));
    }

    @Test
    @DisplayName("No deja guardar una reserva EN_ESPERA sin posición de espera")
    void enEsperaSinPosicionNoSeGuarda() {
        Clase clase = guardarClase();

        assertThrows(Exception.class,
                () -> reservaRepository.saveAndFlush(reserva(clase, 5L, EstadoReserva.EN_ESPERA, null)));
    }
}
