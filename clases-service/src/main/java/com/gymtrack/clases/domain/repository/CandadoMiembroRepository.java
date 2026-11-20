package com.gymtrack.clases.domain.repository;
import com.gymtrack.clases.domain.model.CandadoMiembro;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface CandadoMiembroRepository extends JpaRepository<CandadoMiembro,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CandadoMiembro c where c.miembroId=:id")
 Optional<CandadoMiembro> bloquear(@Param("id") Long id);
}
