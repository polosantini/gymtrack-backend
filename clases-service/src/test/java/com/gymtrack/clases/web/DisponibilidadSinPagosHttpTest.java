package com.gymtrack.clases.web;

import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

/** Clases real + cliente REST real + Miembros HTTP simulado. No se inicia pagos-service. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.datasource.url=jdbc:h2:mem:disponibilidad;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class DisponibilidadSinPagosHttpTest {
    private static final AtomicBoolean activa=new AtomicBoolean(true);
    private static final AtomicInteger estado=new AtomicInteger(200);
    private static final AtomicInteger consultas=new AtomicInteger();
    private static final HttpServer miembros=iniciarMiembros();
    @Autowired private TestRestTemplate http;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @Autowired private CancelacionProcesadaRepository procesadas;
    private Long claseId;
    private final LocalDate fecha=LocalDate.of(2026,11,24);

    private static HttpServer iniciarMiembros() {
        try {
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/api/miembros/6/acceso", exchange->{
                consultas.incrementAndGet();byte[] json=("{\"activa\":"+activa.get()+",\"plan\":\"PREMIUM\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(estado.get(),json.length);exchange.getResponseBody().write(json);exchange.close();
            });server.start();return server;
        } catch(IOException ex) {throw new ExceptionInInitializerError(ex);}
    }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("miembros.base-url",()->"http://127.0.0.1:"+miembros.getAddress().getPort());
    }
    @AfterAll static void detener() {miembros.stop(0);}
    @BeforeEach void preparar() {
        activa.set(true);estado.set(200);consultas.set(0);
        procesadas.deleteAll();reservas.deleteAll();clases.deleteAll();
        var c=new Clase();c.setNombre("Disponibilidad");c.setInstructor("Prueba");c.setDias(Set.of(DiaSemana.MARTES));
        c.setHora(LocalTime.of(18,0));c.setDuracionMinutos(45);c.setAforo(10);c.setSala("Prueba");claseId=clases.save(c).getId();
    }
    private ResponseEntity<String> reservar() {
        return http.postForEntity("/api/reservas",Map.of("miembroId",6,"claseId",claseId,"fecha",fecha.toString()),String.class);
    }
    private void catalogoDisponible() {
        var r=http.getForEntity("/api/clases?fecha="+fecha,String.class);
        assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().contains("Disponibilidad"));
    }
    @Test void catalogoYReservaFuncionanSinServicioDePagos() {
        catalogoDisponible();var r=reservar();assertEquals(201,r.getStatusCode().value());
        assertTrue(r.getBody().contains("CONFIRMADA"));assertEquals(1,reservas.count());assertEquals(1,consultas.get());catalogoDisponible();
    }
    @Test void inactivoEsErrorControladoYElCatalogoSigueVivo() {
        activa.set(false);var r=reservar();assertEquals(409,r.getStatusCode().value());
        assertTrue(r.getBody().contains("La membresía no está activa"));assertTrue(r.getBody().contains("/api/reservas"));
        assertEquals(0,reservas.count());catalogoDisponible();
    }
    @Test void falloDeMiembrosEs503ControladoSinRomperClases() {
        estado.set(500);var r=reservar();assertEquals(503,r.getStatusCode().value());
        assertTrue(r.getBody().contains("No se pudo validar la membresía, intente de nuevo"));
        assertEquals(0,reservas.count());catalogoDisponible();
    }
}
