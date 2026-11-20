# ADR-011: Observer para la cancelación y asignación de cupo solo por evento

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #14b, #33, #34, #35

## Contexto

Cancelar una reserva debe liberar el cupo y avisar a quien esté en espera, sin acoplar el endpoint a esa lógica.

## Decisión

`SujetoReservas` notifica a sus observadores. Uno publica `reserva.cancelada` (reservaId, claseId, fecha, liberaCupo) después del commit. Clases consume el evento y, si `liberaCupo=true`, pasa la primera EN_ESPERA de esa (clase, fecha) a CONFIRMADA bajo bloqueo y publica `reserva.cupo.asignado`. El endpoint nunca asigna el cupo directamente.

## Alternativas consideradas

Asignar el cupo dentro del `PATCH /cancelar` (acopla y se salta el patrón y la mensajería).

## Consecuencias

El flujo queda desacoplado y es la evidencia del patrón. Es más difícil de seguir al depurar y exige idempotencia (ADR-005).
