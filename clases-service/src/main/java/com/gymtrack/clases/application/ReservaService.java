package com.gymtrack.clases.application;

import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReservaService {

    /** Crea la reserva: CONFIRMADA si hay cupo en (clase, fecha), EN_ESPERA con su posición si no. */
    ReservaResponse reservar(ReservaRequest request);

    /** Reservas e historial de asistencia del miembro (incluye canceladas), de la más reciente a la más antigua. */
    List<ReservaResponse> listarPorMiembro(Long miembroId);

    /** Confirmados y lista de espera (ordenada por posición) de una sesión (clase, fecha). */
    ReservasClaseResponse listarPorClaseYFecha(Long claseId, LocalDate fecha);
}
