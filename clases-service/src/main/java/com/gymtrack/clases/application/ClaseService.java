package com.gymtrack.clases.application;

import com.gymtrack.clases.web.dto.ClaseRequest;
import com.gymtrack.clases.web.dto.ClaseResponse;

import java.time.LocalDate;
import java.util.List;

public interface ClaseService {

    ClaseResponse crear(ClaseRequest request);

    /**
     * Catálogo de clases con cupos libres.
     *
     * @param fecha si viene, solo las clases que se dictan ese día y sus cupos de esa fecha;
     *              si es null, todas las clases con los cupos de su próxima sesión desde hoy
     */
    List<ClaseResponse> listar(LocalDate fecha);

    ClaseResponse obtenerPorId(Long id);
}
