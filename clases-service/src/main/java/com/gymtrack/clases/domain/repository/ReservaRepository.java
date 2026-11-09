package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Cuántas reservas hay en un estado para una sesión (clase, fecha). Sirve para calcular el aforo. */
    long countByClaseIdAndFechaAndEstado(Long claseId, LocalDate fecha, EstadoReserva estado);

    /** True si el miembro ya tiene una reserva (en un estado distinto al dado) para esa clase y fecha. */
    boolean existsByMiembroIdAndClaseIdAndFechaAndEstadoNot(Long miembroId, Long claseId, LocalDate fecha, EstadoReserva estado);
}
