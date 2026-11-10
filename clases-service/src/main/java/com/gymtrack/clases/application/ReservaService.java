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

    /** Cancela la reserva (estado CANCELADA). Si estaba en espera, los que venían detrás suben un puesto. */
    ReservaResponse cancelar(Long reservaId);

    /** Marca si el miembro asistió o no. Solo para reservas CONFIRMADA. */
    ReservaResponse marcarAsistencia(Long reservaId, boolean asistio);
}
