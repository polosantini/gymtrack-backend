# ADR-019: CI obligatoria y protección de `main` en ambos repos

- **Estado:** Aceptada
- **Fecha:** 2026-11-05
- **Issues relacionados:** #4, #5, #7

## Contexto

Con 3 personas integrando en paralelo, un push roto a `main` bloquea a todo el equipo.

## Decisión

Workflow de CI en cada PR y push a `main`: backend con Java 21 (temurin), caché de Maven y `mvn -B verify`; frontend con Node, `npm ci` y `npm run build` (verifica tipos con tsc). `main` exige PR, CI en verde y 1 aprobación, y no permite push directo.

## Alternativas consideradas

Revisión informal sin reglas de rama (más rápido, pero sin garantías).

## Consecuencias

Solo llega código verificado a `main`. Cada cambio necesita revisión.
