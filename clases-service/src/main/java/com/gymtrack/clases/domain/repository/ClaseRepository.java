package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.Clase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    /** Debe ejecutarse dentro de la misma transacción que el conteo y el INSERT de la reserva. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> findByIdForUpdate(@Param("id") Long id);
}
