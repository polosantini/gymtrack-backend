package com.gymtrack.clases.domain.exception;

import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.DiaSemana;

import java.time.LocalDate;

/** La fecha pedida cae en un día de la semana en el que la clase no se dicta. */
public class FechaNoCorrespondeException extends RuntimeException {
    public FechaNoCorrespondeException(Clase clase, LocalDate fecha) {
        super("La clase " + clase.getNombre() + " no se dicta el " + fecha
                + " (" + DiaSemana.de(fecha) + "). Días de la clase: " + clase.getDias());
    }
}
