package com.gymtrack.pagos.application;


import com.gymtrack.pagos.web.dto.PagoRequest;
import com.gymtrack.pagos.web.dto.PagoResponse;

import java.util.List;

public interface PagoService {
    PagoResponse crearPago(PagoRequest request);
    List<PagoResponse> listarPorMiembro(Long miembroId);
}
