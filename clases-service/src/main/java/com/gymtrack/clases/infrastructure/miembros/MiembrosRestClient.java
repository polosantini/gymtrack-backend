package com.gymtrack.clases.infrastructure.miembros;

import com.gymtrack.clases.application.port.AccesoMembresia;
import com.gymtrack.clases.application.port.MiembrosClient;
import com.gymtrack.clases.domain.exception.MiembroNoEncontradoException;
import com.gymtrack.clases.domain.exception.MiembrosNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MiembrosRestClient implements MiembrosClient {
    private final RestClient http;

    public MiembrosRestClient(@Qualifier("miembrosHttpClient") RestClient http) {
        this.http = http;
    }

    @Override
    public AccesoMembresia consultarAcceso(Long miembroId) {
        try {
            AccesoMembresia acceso = http.get().uri("/api/miembros/{id}/acceso", miembroId)
                    .retrieve().body(AccesoMembresia.class);
            if (acceso == null || acceso.activa() == null) {
                throw new MiembrosNoDisponibleException();
            }
            return acceso;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new MiembroNoEncontradoException();
            }
            throw new MiembrosNoDisponibleException();
        } catch (RestClientException ex) {
            throw new MiembrosNoDisponibleException();
        }
    }
}
