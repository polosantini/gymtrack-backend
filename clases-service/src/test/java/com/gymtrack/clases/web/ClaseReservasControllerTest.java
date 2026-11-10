package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ClaseService;
import com.gymtrack.clases.application.ClaseQueryService;
import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.EstadoReservaInvalidoException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.exception.ReservaNotFoundException;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaseController.class)
class ClaseReservasControllerTest {
    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);
    @Autowired private MockMvc mockMvc;
    @MockBean private ClaseService claseService;
    @MockBean private ClaseQueryService reservaService;
    @Test
    @DisplayName("GET /api/clases/{id}/reservas separa confirmados y enEspera")
    void reservasDeClaseSeparadas() throws Exception {
        ReservaResponse confirmada = new ReservaResponse(1L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "CONFIRMADA", null, null);
        ReservaResponse espera = new ReservaResponse(4L, 4L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "EN_ESPERA", 1, null);
        when(reservaService.listarPorClaseYFecha(1L, MARTES_17)).thenReturn(new ReservasClaseResponse(
                1L, "Spinning", MARTES_17, LocalTime.of(18, 0), 3, 2, List.of(confirmada), List.of(espera)));

        mockMvc.perform(get("/api/clases/1/reservas").param("fecha", "2026-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmados[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.enEspera[0].posicionEspera").value(1));
    }
    @Test
    @DisplayName("GET /api/clases/{id}/reservas con fecha inválida responde 400")
    void reservasDeClaseFechaInvalidaEs400() throws Exception {
        mockMvc.perform(get("/api/clases/1/reservas").param("fecha", "2026-13-45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
    @Test
    @DisplayName("GET /api/clases/{id}/reservas con clase inexistente responde 404")
    void reservasDeClaseInexistenteEs404() throws Exception {
        when(reservaService.listarPorClaseYFecha(99L, MARTES_17)).thenThrow(new ClaseNotFoundException(99L));

        mockMvc.perform(get("/api/clases/99/reservas").param("fecha", "2026-11-17"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
