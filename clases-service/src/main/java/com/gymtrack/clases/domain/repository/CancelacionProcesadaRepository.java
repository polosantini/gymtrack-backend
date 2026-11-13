package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.CancelacionProcesada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CancelacionProcesadaRepository extends JpaRepository<CancelacionProcesada, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CancelacionProcesada c where c.reservaId = :reservaId")
    Optional<CancelacionProcesada> findByIdForUpdate(@Param("reservaId") Long reservaId);
}
