package com.gymtrack.clases.infrastructure.persistence;

import com.gymtrack.clases.domain.model.DiaSemana;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda los días de una clase en una sola columna de texto,
 * por ejemplo "LUNES,MIERCOLES,VIERNES", y los vuelve a leer como Set.
 * Así no hace falta una tabla extra solo para los días.
 */
@Converter
public class DiasSemanaConverter implements AttributeConverter<Set<DiaSemana>, String> {

    private static final String SEPARADOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<DiaSemana> dias) {
        if (dias == null || dias.isEmpty()) {
            return "";
        }
        // EnumSet ordena siempre de LUNES a DOMINGO
        return EnumSet.copyOf(dias).stream()
                .map(Enum::name)
                .collect(Collectors.joining(SEPARADOR));
    }

    @Override
    public Set<DiaSemana> convertToEntityAttribute(String columna) {
        EnumSet<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        if (columna == null || columna.isBlank()) {
            return dias;
        }
        Arrays.stream(columna.split(SEPARADOR))
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .map(DiaSemana::valueOf)
                .forEach(dias::add);
        return dias;
    }
}
