package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ReservaService;
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
                new ReservaResponse(4L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "EN_ESPERA", 1, null));

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
    @DisplayName("GET /api/reservas?miembroId= incluye asistencia (null si no se ha marcado)")
    void listarPorMiembroIncluyeAsistencia() throws Exception {
        when(reservaService.listarPorMiembro(1L)).thenReturn(List.of(
                new ReservaResponse(1L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "CONFIRMADA", null, null)));

        mockMvc.perform(get("/api/reservas").param("miembroId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$[0].asistencia").value(nullValue()));
    }

    @Test
    @DisplayName("GET /api/reservas sin miembroId responde 400")
    void listarSinMiembroIdEs400() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro requerido: miembroId"));
    }




    @Test
    @DisplayName("PATCH /api/reservas/{id}/cancelar responde 200 con la reserva CANCELADA")
    void cancelarResponde200() throws Exception {
        when(reservaService.cancelar(1L)).thenReturn(
                new ReservaResponse(1L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "CANCELADA", null, null));

        mockMvc.perform(patch("/api/reservas/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/cancelar de una reserva inexistente responde 404 con mensaje legible")
    void cancelarInexistenteEs404() throws Exception {
        when(reservaService.cancelar(99L)).thenThrow(new ReservaNotFoundException(99L));

        mockMvc.perform(patch("/api/reservas/99/cancelar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Reserva no encontrada con id: 99"))
                .andExpect(jsonPath("$.path").value("/api/reservas/99/cancelar"));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/cancelar de una reserva ya cancelada responde 409")
    void cancelarYaCanceladaEs409() throws Exception {
        when(reservaService.cancelar(1L)).thenThrow(new EstadoReservaInvalidoException("La reserva 1 ya está cancelada"));

        mockMvc.perform(patch("/api/reservas/1/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La reserva 1 ya está cancelada"));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/asistencia con {asistio: true} responde 200 con asistencia true")
    void asistenciaResponde200() throws Exception {
        when(reservaService.marcarAsistencia(1L, true)).thenReturn(
                new ReservaResponse(1L, 1L, 1L, "Spinning", MARTES_17, LocalTime.of(18, 0), "CONFIRMADA", null, true));

        mockMvc.perform(patch("/api/reservas/1/asistencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asistio\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asistencia").value(true));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/asistencia sin el campo asistio responde 400")
    void asistenciaSinCampoEs400() throws Exception {
        mockMvc.perform(patch("/api/reservas/1/asistencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El campo asistio es obligatorio"));
    }

    @Test
    @DisplayName("PATCH /api/reservas/{id}/asistencia de una reserva inexistente responde 404")
    void asistenciaInexistenteEs404() throws Exception {
        when(reservaService.marcarAsistencia(99L, false)).thenThrow(new ReservaNotFoundException(99L));

        mockMvc.perform(patch("/api/reservas/99/asistencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asistio\": false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reserva no encontrada con id: 99"));
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
