package com.gymtrack.clases.application;

import com.gymtrack.clases.web.dto.ClaseResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import java.time.LocalDate;
import java.util.List;

/** ISP: solo lecturas de clases y sus sesiones; no crea, reserva ni cancela. */
public interface ClaseQueryService {
    List<ClaseResponse> listar(LocalDate fecha);
    ClaseResponse obtenerPorId(Long id);
    ReservasClaseResponse listarPorClaseYFecha(Long claseId, LocalDate fecha);
}
