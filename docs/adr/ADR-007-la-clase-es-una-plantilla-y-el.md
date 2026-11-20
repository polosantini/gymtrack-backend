# ADR-007: La clase es una plantilla y el aforo se cuenta por (clase, fecha)

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #14, #15, #16, #17

## Contexto

Una clase se dicta varios días a la semana (por ejemplo, martes y jueves). El aforo no puede ser global a la clase.

## Decisión

`Clase` guarda instructor, días, hora, duración, aforo y sala. La `Reserva` lleva la fecha. Los cupos libres son aforo menos las reservas CONFIRMADA de esa (clase, fecha). Se rechaza una fecha que no corresponde a un día de la clase.

## Alternativas consideradas

Una fila de clase por sesión (duplica datos y complica el catálogo).

## Consecuencias

Un solo catálogo y el aforo se calcula por sesión. Toda consulta y regla debe incluir la fecha.
