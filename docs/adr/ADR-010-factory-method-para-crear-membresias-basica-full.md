# ADR-010: Factory Method para crear membresías Básica, Full y Premium

- **Estado:** Aceptada
- **Fecha:** 2026-11-05
- **Issues relacionados:** #21, #37

## Contexto

El curso exige Factory Method, y crear cada plan con if/switch viola OCP.

## Decisión

Interfaz `PlanMembresia` y `CreadorMembresia` con creadores concretos para BASICO, FULL y PREMIUM. `POST /api/miembros/{id}/membresias` usa la fábrica y la membresía nace SUSPENDIDA. Los 3 planes responden igual a la pregunta de acceso (LSP).

## Alternativas consideradas

Enum con switch (simple, pero cierra el modelo a extensión).

## Consecuencias

Un plan nuevo no obliga a tocar los creadores existentes y existe una prueba de LSP. El modelo tiene más clases que un enum.
