package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ClaseService;
import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.model.DiaSemana;
import com.gymtrack.clases.web.dto.ClaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaseController.class)
class ClaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaseService claseService;

    private static final String CLASE_VALIDA = """
            {
              "nombre": "Spinning",
              "instructor": "Carlos Ruiz",
              "dias": ["MARTES", "jueves"],
              "hora": "18:00",
              "duracionMinutos": 45,
              "aforo": 3,
              "sala": "Sala 1"
            }
            """;

    private ClaseResponse respuestaSpinning() {
        return new ClaseResponse(1L, "Spinning", "Carlos Ruiz", EnumSet.of(DiaSemana.MARTES, DiaSemana.JUEVES),
                LocalTime.of(18, 0), 45, 3, "Sala 1", LocalDate.of(2026, 11, 17), 3);
    }

    @Test
    @DisplayName("POST /api/clases válido responde 201 con la clase creada")
    void crearClaseValida() throws Exception {
        when(claseService.crear(any())).thenReturn(respuestaSpinning());

        mockMvc.perform(post("/api/clases").contentType(MediaType.APPLICATION_JSON).content(CLASE_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hora").value("18:00"))
                .andExpect(jsonPath("$.dias[0]").value("MARTES"))
                .andExpect(jsonPath("$.cuposLibres").value(3));
    }

    @Test
    @DisplayName("POST /api/clases con aforo 0 responde 400 con formato uniforme")
    void aforoCeroEs400() throws Exception {
        String json = CLASE_VALIDA.replace("\"aforo\": 3", "\"aforo\": 0");

        mockMvc.perform(post("/api/clases").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El aforo debe ser mayor que cero"))
                .andExpect(jsonPath("$.path").value("/api/clases"));
    }

    @Test
    @DisplayName("POST /api/clases sin días responde 400")
    void diasVaciosEs400() throws Exception {
        String json = CLASE_VALIDA.replace("[\"MARTES\", \"jueves\"]", "[]");

        mockMvc.perform(post("/api/clases").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe indicar al menos un día"));
    }

    @Test
    @DisplayName("POST /api/clases con hora inválida responde 400")
    void horaInvalidaEs400() throws Exception {
        String json = CLASE_VALIDA.replace("\"18:00\"", "\"25:99\"");

        mockMvc.perform(post("/api/clases").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/clases?fecha= devuelve el catálogo")
    void listarCatalogo() throws Exception {
        when(claseService.listar(LocalDate.of(2026, 11, 17))).thenReturn(List.of(respuestaSpinning()));

        mockMvc.perform(get("/api/clases").param("fecha", "2026-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Spinning"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-17"));
    }

    @Test
    @DisplayName("GET /api/clases con fecha mal escrita responde 400")
    void fechaMalEscritaEs400() throws Exception {
        mockMvc.perform(get("/api/clases").param("fecha", "17-11-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/clases/{id} inexistente responde 404 con formato uniforme")
    void claseInexistenteEs404() throws Exception {
        when(claseService.obtenerPorId(99L)).thenThrow(new ClaseNotFoundException(99L));

        mockMvc.perform(get("/api/clases/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Clase no encontrada con id: 99"));
    }
}
