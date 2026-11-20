# ADR-022: Tarea programada diaria para vencimientos con disparo manual

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #32

## Contexto

El flujo `membresia.proxima.vencer` depende del tiempo y no puede esperarse al día correcto durante la demo.

## Decisión

Una tarea `@Scheduled` diaria en `miembros-service` detecta membresías próximas a vencer y publica miembroId, nombre, fechaVencimiento y diasRestantes. Un endpoint manual obligatorio dispara la tarea en la demo. No se publican eventos duplicados para la misma membresía el mismo día.

## Alternativas consideradas

Solo la tarea programada (no demostrable en vivo).

## Consecuencias

La demo puede mostrar el flujo en cualquier momento. El endpoint manual debe protegerse o retirarse fuera de la demo.
