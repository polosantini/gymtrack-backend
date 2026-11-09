package com.gymtrack.miembros.domain.plan;

import com.gymtrack.miembros.domain.model.TipoPlanMembresia;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreadorMembresia {

    private final Map<TipoPlanMembresia, PlanMembresia> planes;

    public CreadorMembresia() {
        this.planes = Map.of(
                TipoPlanMembresia.BASICO, new PlanBasico(),
                TipoPlanMembresia.FULL, new PlanFull(),
                TipoPlanMembresia.PREMIUM, new PlanPremium()
        );
    }

    public PlanMembresia crear(TipoPlanMembresia tipo) {
        PlanMembresia plan = planes.get(tipo);

        if (plan == null) {
            throw new IllegalArgumentException("Plan no soportado: "+tipo);
        }

        return plan;
    }

}
