package com.gymtrack.miembros.domain.plan;

public class PlanPremium extends PlanMembresiaBase{
    @Override
    public String getNombre() {
        return "PREMIUM";
    }

    @Override
    public int diasVigencia() {
        return 365;
    }
}
