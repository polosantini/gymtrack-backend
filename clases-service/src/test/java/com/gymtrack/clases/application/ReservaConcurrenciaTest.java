package com.gymtrack.clases.application;

import com.gymtrack.clases.application.port.*;
import com.gymtrack.clases.domain.model.*;
import com.gymtrack.clases.domain.repository.*;
import com.gymtrack.clases.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencia;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
class ReservaConcurrenciaTest {
    @Autowired private ReservaService servicio;
    @Autowired private ClaseRepository clases;
    @Autowired private ReservaRepository reservas;
    @MockBean private MiembrosClient miembros;
    private final LocalDate fecha = LocalDate.of(2026, 11, 17);
    private Long claseId;

    @BeforeEach
    void preparar() {
        reservas.deleteAll(); clases.deleteAll();
        when(miembros.consultarAcceso(anyLong())).thenReturn(new AccesoMembresia(true, "PREMIUM"));
        Clase clase = new Clase(); clase.setNombre("Último cupo"); clase.setInstructor("Instructor");
        clase.setDias(Set.of(DiaSemana.MARTES)); clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45); clase.setAforo(1); clase.setSala("Sala");
        claseId = clases.save(clase).getId();
    }

    @RepeatedTest(3)
    void ochoHilosCompitenPorUnCupoSinSobreventa() throws Exception {
        int hilos = 8;
        CyclicBarrier inicio = new CyclicBarrier(hilos);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<ReservaResponse>> resultados = new ArrayList<>();
            for (long i = 1; i <= hilos; i++) {
                long miembro = i;
                resultados.add(pool.submit(() -> {
                    inicio.await(10, TimeUnit.SECONDS);
                    return servicio.reservar(new ReservaRequest(miembro, claseId, fecha));
                }));
            }
            List<ReservaResponse> respuestas = new ArrayList<>();
            for (var resultado : resultados) respuestas.add(resultado.get(20, TimeUnit.SECONDS));
            assertEquals(1, respuestas.stream().filter(r -> r.estado().equals("CONFIRMADA")).count());
            assertEquals(7, respuestas.stream().filter(r -> r.estado().equals("EN_ESPERA")).count());
            assertEquals(7, respuestas.stream().filter(r -> r.estado().equals("EN_ESPERA"))
                    .map(ReservaResponse::posicionEspera).distinct().count());
            assertEquals(1, reservas.countByClaseIdAndFechaAndEstado(claseId, fecha, EstadoReserva.CONFIRMADA));
            assertEquals(8, reservas.count());
        } finally {
            pool.shutdownNow();
        }
    }
}
