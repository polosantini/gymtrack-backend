package com.gymtrack.miembros.domain.plan;

import com.gymtrack.miembros.domain.model.EstadoMembresia;

public abstract class PlanMembresiaBase implements PlanMembresia {

    @Override
    public boolean puedeAcceder(EstadoMembresia estado) {
        return estado == EstadoMembresia.ACTIVA;
    }

    @Override
    public abstract String getNombre();

    @Override
    public abstract int diasVigencia();
}
