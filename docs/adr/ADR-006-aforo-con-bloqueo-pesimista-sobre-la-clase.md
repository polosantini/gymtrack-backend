# ADR-006: Aforo con bloqueo pesimista sobre la clase

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #27, #38

## Contexto

El atributo de consistencia exige que, si varios hilos piden el último cupo, solo uno quede CONFIRMADA y el resto EN_ESPERA.

## Decisión

`@Lock(PESSIMISTIC_WRITE)` sobre la clase, tomado antes de contar las confirmadas. El conteo y la decisión (CONFIRMADA o EN_ESPERA) ocurren en la misma transacción. Se valida con una prueba de unos 10 hilos.

## Alternativas consideradas

Bloqueo optimista con reintentos (más conflictos bajo contención); restricción única en base de datos (no cubre la regla de aforo).

## Consecuencias

No hay sobreventa. Se serializan las reservas de una misma clase, algo aceptable con aforos pequeños. El trade-off se documenta en el informe.
