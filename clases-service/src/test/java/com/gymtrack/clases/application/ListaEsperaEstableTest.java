package com.gymtrack.clases.application;

import com.gymtrack.clases.application.port.*;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.clases.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:espera;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class ListaEsperaEstableTest {
    @Autowired private ReservaService servicio;
    @Autowired private ClaseQueryService consulta;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @MockBean private MiembrosClient miembros;
    private final LocalDate fecha = LocalDate.of(2026, 11, 17);
    private Long claseId;

    @BeforeEach
    void preparar() {
        reservas.deleteAll(); clases.deleteAll();
        when(miembros.consultarAcceso(anyLong())).thenReturn(new AccesoMembresia(true, "PREMIUM"));
        Clase clase = new Clase(); clase.setNombre("Cola"); clase.setInstructor("Instructor");
        clase.setDias(Set.of(DiaSemana.MARTES)); clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45); clase.setAforo(1); clase.setSala("Sala");
        claseId = clases.save(clase).getId();
    }
    private ReservaResponse reservar(long miembro) {
        return servicio.reservar(new ReservaRequest(miembro, claseId, fecha));
    }
    private List<Integer> posiciones() {
        return consulta.listarPorClaseYFecha(claseId, fecha).enEspera().stream().map(ReservaResponse::posicionEspera).toList();
    }
    @Test
    void cancelarDejaHuecoYLaSiguienteLlegadaNoRepitePosicion() {
        reservar(1); var primera = reservar(2); reservar(3); reservar(4);
        assertEquals(List.of(1, 2, 3), posiciones());
        servicio.cancelar(primera.id());
        assertEquals(List.of(2, 3), posiciones());
        assertEquals(List.of(2, 3), posiciones()); // Consultar tampoco renumera.
        assertEquals(4, reservar(5).posicionEspera());
        assertEquals(List.of(2, 3, 4), posiciones());
        assertEquals(EstadoReserva.CANCELADA, reservas.findById(primera.id()).orElseThrow().getEstado());
    }
    @Test
    void unaLlegadaNuevaNoSeSaltaLaColaAlLiberarseUnCupo() {
        var confirmada = reservar(1); reservar(2);
        servicio.cancelar(confirmada.id());
        var nueva = reservar(3);
        assertEquals("EN_ESPERA", nueva.estado()); assertEquals(2, nueva.posicionEspera());
        // La promoción automática se conecta en Fase 3; este PR preserva la prioridad.
        assertEquals(0, consulta.listarPorClaseYFecha(claseId, fecha).confirmados().size());
    }
    @Test
    void duplicadoActivoSeRechazaPeroCanceladoPuedeVolverAlFinal() {
        reservar(1); var espera = reservar(2); reservar(3);
        assertThrows(ReservaDuplicadaException.class, () -> reservar(2));
        servicio.cancelar(espera.id());
        assertEquals(3, reservar(2).posicionEspera());
        assertEquals(List.of(2, 3), posiciones());
    }
}
