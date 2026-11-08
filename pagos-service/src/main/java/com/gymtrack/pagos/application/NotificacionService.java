package com.gymtrack.pagos.application;

import com.gymtrack.pagos.web.dto.NotificacionResponse;

import java.util.List;

public interface NotificacionService {
    List<NotificacionResponse> listarPorMiembro(Long miembroId);
}
