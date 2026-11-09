package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.model.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Conteo por miembro y rango INCLUSIVO, entre todas las clases, solo en los estados indicados. */
    long countByMiembroIdAndFechaBetweenAndEstadoIn(Long miembroId, LocalDate inicio, LocalDate fin,
                                                  Collection<EstadoReserva> estados);


    /** Cuántas reservas hay en un estado para una sesión (clase, fecha). Sirve para calcular el aforo. */
    long countByClaseIdAndFechaAndEstado(Long claseId, LocalDate fecha, EstadoReserva estado);

    /** True si el miembro ya tiene una reserva (en un estado distinto al dado) para esa clase y fecha. */
    boolean existsByMiembroIdAndClaseIdAndFechaAndEstadoNot(Long miembroId, Long claseId, LocalDate fecha, EstadoReserva estado);

    /** Historial del miembro, de la fecha más reciente a la más antigua. Incluye canceladas. */
    @EntityGraph(attributePaths = "clase")
    List<Reserva> findByMiembroIdOrderByFechaDescIdDesc(Long miembroId);

    /** Reservas de una sesión en un estado, en orden de creación (para los confirmados). */
    @EntityGraph(attributePaths = "clase")
    List<Reserva> findByClaseIdAndFechaAndEstadoOrderByIdAsc(Long claseId, LocalDate fecha, EstadoReserva estado);

    /** Reservas de una sesión en un estado ordenadas por posición de espera (nunca por id). */
    @EntityGraph(attributePaths = "clase")
    List<Reserva> findByClaseIdAndFechaAndEstadoOrderByPosicionEsperaAsc(Long claseId, LocalDate fecha, EstadoReserva estado);
}
