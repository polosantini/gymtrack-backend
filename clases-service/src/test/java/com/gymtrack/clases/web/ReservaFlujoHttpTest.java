package com.gymtrack.clases.web;

import com.gymtrack.clases.application.port.*;
import com.gymtrack.clases.domain.exception.*;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:flujo;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class ReservaFlujoHttpTest {
    @Autowired private TestRestTemplate http;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @MockBean private MiembrosClient miembros;
    @MockBean private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    private final LocalDate fecha = LocalDate.of(2026, 11, 17);

    @BeforeEach
    void preparar() {
        http.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        reservas.deleteAll(); clases.deleteAll();
        when(miembros.consultarAcceso(anyLong())).thenAnswer(inv -> {
            String plan = Map.of(101L, "BASICO", 102L, "FULL", 103L, "PREMIUM").get(inv.<Long>getArgument(0));
            return new AccesoMembresia(true, plan);
        });
    }

    private Long clase() {
        Clase clase = new Clase(); clase.setNombre("Prueba"); clase.setInstructor("Instructor");
        clase.setDias(EnumSet.allOf(DiaSemana.class)); clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45); clase.setAforo(1); clase.setSala("Sala");
        return clases.save(clase).getId();
    }
    private ResponseEntity<String> reservar(Long miembro, Long clase, LocalDate dia) {
        return http.postForEntity("/api/reservas", Map.of("miembroId", miembro, "claseId", clase, "fecha", dia.toString()), String.class);
    }

    @Test
    void basicoDosPermitidasYTerceraRechazadaAunqueFrontendMienta() {
        assertEquals(201, reservar(101L, clase(), fecha).getStatusCode().value());
        assertEquals(201, reservar(101L, clase(), fecha.plusDays(1)).getStatusCode().value());
        var r = http.postForEntity("/api/reservas", Map.of("miembroId", 101L, "claseId", clase(),
                "fecha", fecha.plusDays(2).toString(), "plan", "PREMIUM"), String.class);
        assertEquals(409, r.getStatusCode().value());
        assertTrue(r.getBody().contains("El plan BASICO permite máximo 2 reservas por semana"));
        assertEquals(2, reservas.count());
    }

    @Test
    void esperaCuentaYCanceladaLiberaCuota() {
        Long llena = clase();
        assertEquals(201, reservar(103L, llena, fecha).getStatusCode().value());
        var espera = reservar(101L, llena, fecha);
        assertTrue(espera.getBody().contains("EN_ESPERA"));
        assertEquals(201, reservar(101L, clase(), fecha).getStatusCode().value());
        Long tercera = clase();
        assertEquals(409, reservar(101L, tercera, fecha).getStatusCode().value());
        Long id = reservas.findByMiembroIdOrderByFechaDescIdDesc(101L).stream()
                .filter(r -> r.getEstado() == EstadoReserva.EN_ESPERA).findFirst().orElseThrow().getId();
        assertEquals(200, http.exchange("/api/reservas/" + id + "/cancelar", HttpMethod.PATCH,
                HttpEntity.EMPTY, String.class).getStatusCode().value());
        assertEquals(201, reservar(101L, tercera, fecha).getStatusCode().value());
    }

    @Test
    void fullNoPermiteDosElMismoDiaPeroSiOtroDia() {
        assertEquals(201, reservar(102L, clase(), fecha).getStatusCode().value());
        var r = reservar(102L, clase(), fecha);
        assertEquals(409, r.getStatusCode().value());
        assertTrue(r.getBody().contains("El plan FULL permite máximo 1 reservas por día"));
        assertEquals(201, reservar(102L, clase(), fecha.plusDays(1)).getStatusCode().value());
    }

    @Test
    void premiumSinLimiteDePlanMantieneDuplicadosYAforo() {
        for (int i = 0; i < 8; i++) assertEquals(201, reservar(103L, clase(), fecha).getStatusCode().value());
        Long llena = clase();
        reservar(102L, llena, fecha);
        assertTrue(reservar(103L, llena, fecha).getBody().contains("EN_ESPERA"));
        assertEquals(409, reservar(103L, llena, fecha).getStatusCode().value());
    }

    @Test
    void miembroInactivoDevuelve409SinGuardar() {
        when(miembros.consultarAcceso(101L)).thenReturn(new AccesoMembresia(false, "BASICO"));
        var r = reservar(101L, clase(), fecha);
        assertEquals(409, r.getStatusCode().value());
        assertTrue(r.getBody().contains("La membresía no está activa")); assertEquals(0, reservas.count());
    }

    @Test
    void miembrosCaidoDevuelve503SinGuardar() {
        when(miembros.consultarAcceso(101L)).thenThrow(new MiembrosNoDisponibleException());
        var r = reservar(101L, clase(), fecha);
        assertEquals(503, r.getStatusCode().value());
        assertTrue(r.getBody().contains("No se pudo validar la membresía, intente de nuevo")); assertEquals(0, reservas.count());
    }

    @Test
    void miembroInexistenteDevuelve404() {
        when(miembros.consultarAcceso(101L)).thenThrow(new MiembroNoEncontradoException());
        assertEquals(404, reservar(101L, clase(), fecha).getStatusCode().value());
    }

    @Test
    void planDesconocidoNoSeConvierteEnPremium() {
        when(miembros.consultarAcceso(101L)).thenReturn(new AccesoMembresia(true, "DESCONOCIDO"));
        assertEquals(503, reservar(101L, clase(), fecha).getStatusCode().value());
        assertEquals(0, reservas.count());
    }
}
