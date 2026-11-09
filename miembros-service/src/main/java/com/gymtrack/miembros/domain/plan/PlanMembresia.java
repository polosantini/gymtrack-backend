package com.gymtrack.miembros.domain.plan;

import com.gymtrack.miembros.domain.model.EstadoMembresia;

public interface PlanMembresia {
    String getNombre();
    boolean puedeAcceder(EstadoMembresia estado);
    int diasVigencia();
}