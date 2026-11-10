package com.gymtrack.clases.application;

import com.gymtrack.clases.web.dto.ClaseRequest;
import com.gymtrack.clases.web.dto.ClaseResponse;

/** Operaciones de escritura de clases, separadas de su consulta. */
public interface ClaseService {
    ClaseResponse crear(ClaseRequest request);
}
