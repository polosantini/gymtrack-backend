package com.gymtrack.clases.application;

import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;

public interface ReservaService {

    /** Crea la reserva: CONFIRMADA si hay cupo en (clase, fecha), EN_ESPERA con su posición si no. */
    ReservaResponse reservar(ReservaRequest request);
}
