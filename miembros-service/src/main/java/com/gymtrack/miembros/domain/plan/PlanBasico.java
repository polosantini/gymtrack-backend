package com.gymtrack.miembros.domain.plan;

public class PlanBasico extends PlanMembresiaBase {
    @Override
    public String getNombre() {
        return "BASICO";
    }

    @Override
    public int diasVigencia() {
        return 30;
    }

}
