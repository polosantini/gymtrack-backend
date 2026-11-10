package com.gymtrack.clases.infrastructure.miembros;

import com.gymtrack.clases.domain.exception.MiembroNoEncontradoException;
import com.gymtrack.clases.domain.exception.MiembrosNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MiembrosRestClientTest {
    private MockRestServiceServer server;
    private MiembrosRestClient client;
    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("http://localhost:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MiembrosRestClient(builder.build());
    }
    private void respuesta(String json) {
        server.expect(requestTo("http://localhost:8081/api/miembros/2/acceso"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
    @Test
    void consultaEndpointDelContratoYConservaPlan() {
        respuesta("{\"activa\":true,\"plan\":\"FULL\"}");
        var acceso = client.consultarAcceso(2L);
        assertTrue(acceso.activa()); assertEquals("FULL", acceso.plan()); server.verify();
    }
    @Test
    void permiteRespuestaInactivaSinPlanParaQueElServicioLaRechaceCon409() {
        respuesta("{\"activa\":false,\"plan\":null}");
        assertFalse(client.consultarAcceso(2L).activa()); server.verify();
    }
    @Test
    void miembroInexistente() {
        server.expect(requestTo("http://localhost:8081/api/miembros/2/acceso")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThrows(MiembroNoEncontradoException.class, () -> client.consultarAcceso(2L));
    }
    @Test
    void servidorCaido() {
        server.expect(requestTo("http://localhost:8081/api/miembros/2/acceso")).andRespond(withException(new IOException("connection refused")));
        var ex = assertThrows(MiembrosNoDisponibleException.class, () -> client.consultarAcceso(2L));
        assertEquals("No se pudo validar la membresía, intente de nuevo", ex.getMessage());
    }
    @Test
    void errorDelServidorNoSeExpone() {
        server.expect(requestTo("http://localhost:8081/api/miembros/2/acceso")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(MiembrosNoDisponibleException.class, () -> client.consultarAcceso(2L));
    }
    @Test
    void respuestaSinActivaNoAutorizaReservas() {
        respuesta("{\"plan\":\"PREMIUM\"}");
        assertThrows(MiembrosNoDisponibleException.class, () -> client.consultarAcceso(2L));
    }
    @Test
    void jsonInvalidoNoExponeExcepcionCruda() {
        respuesta("{invalido");
        assertThrows(MiembrosNoDisponibleException.class, () -> client.consultarAcceso(2L));
    }
}
