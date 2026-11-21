# ADR-012: Lista de espera con posición persistida y estable

- **Estado:** Aceptada
- **Fecha:** 2026-11-05
- **Issues relacionados:** #16, #17, #28, #34

## Contexto

Si la posición se recalcula al consultar o al cancelar otras reservas, el paso 4 de la demo se rompe.

## Decisión

La posición se guarda en la reserva por orden de llegada y no se recalcula. La lista se devuelve ordenada por posición, nunca por id. Reservar dos veces la misma (clase, fecha) devuelve 409.

## Alternativas consideradas

Calcular la posición al vuelo por fecha de creación (más simple, pero inestable).

## Consecuencias

Posiciones predecibles. Pueden quedar huecos en la numeración tras cancelaciones.
