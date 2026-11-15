package com.gymtrack.pagos.domain.repository;

import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByMiembroIdOrderByFechaDesc(Long miembroId);
    boolean existsByTipoAndReferenciaExterna(TipoNotificacion tipo, Long referenciaExterna);
}
