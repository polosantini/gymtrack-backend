package com.gymtrack.miembros.domain.repository;
import com.gymtrack.miembros.domain.model.EventoPendiente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface EventoPendienteRepository extends JpaRepository<EventoPendiente,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from EventoPendiente e where e.id=:id")
 Optional<EventoPendiente> buscarConLock(@Param("id") String id);
 List<EventoPendiente> findFirst20ByEnviadoFalseOrderByCreadoAsc();
}
