# ADR-009: El plan que se aplica sale siempre de Miembros, nunca del frontend

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #22, #25

## Contexto

Si el cliente enviara su plan, un usuario podría saltarse los límites de Básico o Full.

## Decisión

Clases consulta `/acceso` antes de reservar y usa el `plan` que devuelve Miembros para alimentar la Strategy. La petición solo trae `{miembroId, claseId, fecha}`. Membresía inactiva responde 409 con el mensaje del contrato.

## Alternativas consideradas

Confiar en el plan enviado por el cliente (inseguro); copiar el plan a Clases por eventos (datos desfasados).

## Consecuencias

La fuente de verdad es única. Reservar depende de que Miembros esté disponible (ver ADR-003).
