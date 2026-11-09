package com.gymtrack.miembros.domain.plan;

public class PlanFull extends PlanMembresiaBase {
    @Override
    public String getNombre() {
        return "FULL";
    }

    @Override
    public int diasVigencia() {
        return 90;
    }
}
