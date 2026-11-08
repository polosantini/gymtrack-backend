package com.gymtrack.pagos.application;

import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.repository.NotificacionRepository;
import com.gymtrack.pagos.web.dto.NotificacionResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class NotificacionServiceImpl implements NotificacionService {
    private final NotificacionRepository notificacionRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public List<NotificacionResponse> listarPorMiembro(Long miembroId) {
        return notificacionRepository.findByMiembroIdOrderByFechaDesc(miembroId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getMiembroId(),
                n.getTipo().name(),
                n.getMensaje(),
                n.getFecha(),
                n.isLeida()
        );
    }
}
