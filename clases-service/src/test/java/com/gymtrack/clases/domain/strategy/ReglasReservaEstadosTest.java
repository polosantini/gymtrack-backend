package com.gymtrack.clases.domain.strategy;

import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Complementa las pruebas unitarias existentes verificando los estados contra el repositorio real. */
@DataJpaTest(properties="spring.sql.init.mode=never")
@Import({ReglaBasico.class,ReglaFull.class,ReglaPremium.class})
class ReglasReservaEstadosTest {
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @Autowired private ReglaBasico basico;
    @Autowired private ReglaFull full;
    @Autowired private ReglaPremium premium;
    private final LocalDate fecha=LocalDate.of(2026,11,17);
    private Clase clase;
    private int desplazamiento;
    @BeforeEach void preparar() {
        desplazamiento=0;
        clase=new Clase();clase.setNombre("Prueba de estados");clase.setInstructor("Instructor");
        clase.setDias(EnumSet.allOf(DiaSemana.class));clase.setHora(LocalTime.of(18,0));
        clase.setDuracionMinutos(45);clase.setAforo(20);clase.setSala("Prueba");clase=clases.save(clase);
    }
    // Sesiones distintas: el fixture no crea duplicados activos de la misma clase/fecha.
    private void guardar(EstadoReserva estado) {
        var r=new Reserva();r.setMiembroId(10L);r.setClase(clase);r.setFecha(fecha.plusDays(desplazamiento++));r.setEstado(estado);
        if(estado==EstadoReserva.EN_ESPERA) r.setPosicionEspera(1);
        reservas.saveAndFlush(r);
    }
    @Test void basicoPermiteDosConfirmadasYRechazaLaTercera() {
        assertDoesNotThrow(()->basico.validar(10L,fecha));guardar(EstadoReserva.CONFIRMADA);
        assertDoesNotThrow(()->basico.validar(10L,fecha));guardar(EstadoReserva.CONFIRMADA);
        assertThrows(ReglaReservaIncumplidaException.class,()->basico.validar(10L,fecha));
    }
    @Test void basicoCuentaEnEsperaParaElLimiteSemanal() {
        guardar(EstadoReserva.CONFIRMADA);guardar(EstadoReserva.EN_ESPERA);
        assertThrows(ReglaReservaIncumplidaException.class,()->basico.validar(10L,fecha));
    }
    @Test void basicoNoCuentaCanceladas() {
        guardar(EstadoReserva.CONFIRMADA);guardar(EstadoReserva.CANCELADA);
        assertDoesNotThrow(()->basico.validar(10L,fecha));
    }
    @Test void fullRechazaSegundaConfirmadaDelDia() {
        guardar(EstadoReserva.CONFIRMADA);
        assertThrows(ReglaReservaIncumplidaException.class,()->full.validar(10L,fecha));
    }
    @Test void fullCuentaEnEsperaComoReservaDelDia() {
        guardar(EstadoReserva.EN_ESPERA);
        assertThrows(ReglaReservaIncumplidaException.class,()->full.validar(10L,fecha));
    }
    @Test void fullNoCuentaCanceladas() {
        guardar(EstadoReserva.CANCELADA);
        assertDoesNotThrow(()->full.validar(10L,fecha));
    }
    @Test void premiumNoRechazaAunqueSupereLosLimitesDeOtrosPlanes() {
        for(int i=0;i<5;i++) guardar(EstadoReserva.CONFIRMADA);
        guardar(EstadoReserva.EN_ESPERA);guardar(EstadoReserva.CANCELADA);
        assertDoesNotThrow(()->premium.validar(10L,fecha));
    }
}
