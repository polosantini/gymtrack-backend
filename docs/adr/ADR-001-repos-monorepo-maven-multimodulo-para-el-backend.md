# ADR-001: Repos: monorepo Maven multimódulo para el backend y repo aparte para el frontend

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #1, #3, #6

## Contexto

El equipo es de 3 personas (A y B en backend, C en frontend) con 30 días. Los tres servicios comparten las clases de eventos y necesitan una CI común.

## Decisión

Un repo `backend` Maven multimódulo con `common` (eventos y constantes), `miembros-service`, `clases-service` y `pagos-service`, Java 21. Un repo `frontend` con React + TypeScript + Vite.

## Alternativas consideradas

Un repo por servicio (más repos y más CI, y habría que publicar `common` como artefacto); un monorepo con frontend incluido (mezcla toolchains Maven y Node).

## Consecuencias

Los eventos se comparten sin publicar artefactos y hay una sola CI de backend. Los servicios se versionan juntos, no de forma independiente.
