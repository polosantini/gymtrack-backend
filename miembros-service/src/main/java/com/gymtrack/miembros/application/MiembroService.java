package com.gymtrack.miembros.application;

import com.gymtrack.miembros.web.dto.*;

import java.util.List;

public interface MiembroService {
    MiembroResponse registrar(MiembroRequest request);
    List<MiembroResponse> listar();
    MiembroResponse obtenerPorId(Long id);
    MembresiaResponse crearMembresia(Long miembroId, MembresiaRequest request);
    List<PlanResponse> listarPlanes();
}
