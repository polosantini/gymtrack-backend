package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ReservaService;
import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.web.dto.ReservaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    private static final String RESERVA_VALIDA = """
            { "miembroId": 1, "claseId": 1, "fecha": "2026-11-17" }
            """;

    @Test
    @DisplayName("POST /api/reservas responde 201 con estado EN_ESPERA y posición")
    void reservaEnEspera() throws Exception {
        when(reservaService.reservar(any())).thenReturn(
                new ReservaResponse(4L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "EN_ESPERA", 1));

        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$.posicionEspera").value(1))
                .andExpect(jsonPath("$.fecha").value("2026-11-17"));
    }

    @Test
    @DisplayName("POST /api/reservas sin miembroId responde 400")
    void sinMiembroIdEs400() throws Exception {
        String json = """
                { "claseId": 1, "fecha": "2026-11-17" }
                """;

        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El miembroId es obligatorio"));
    }

    @Test
    @DisplayName("POST /api/reservas con clase inexistente responde 404")
    void claseInexistenteEs404() throws Exception {
        when(reservaService.reservar(any())).thenThrow(new ClaseNotFoundException(1L));

        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/reservas duplicada responde 409")
    void duplicadaEs409() throws Exception {
        when(reservaService.reservar(any())).thenThrow(new ReservaDuplicadaException(1L, 1L, MARTES_17));

        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
