package com.gymtrack.clases.application.port;

/** Única consulta REST entre servicios. El plan nunca procede del frontend. */
public interface MiembrosClient {
    AccesoMembresia consultarAcceso(Long miembroId);
}
