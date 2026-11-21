# ADR-023: Estrategia de ramas: GitHub Flow con main protegida

- **Estado:** Aceptada
- **Fecha:** 2026-10-28

## Contexto

Tres personas trabajan en paralelo en dos repositorios (backend y frontend), con entregas cortas y una sola versión desplegada: la que corre la demo a partir de las imágenes publicadas desde `main`.

## Decisión

Usar GitHub Flow: una rama `main` protegida y siempre desplegable, y ramas cortas por issue (`feature/`, `fix/`, `test/`) que nacen de `main` y vuelven a ella por pull request con CI en verde y una aprobación.

## Alternativas consideradas

Gitflow clásico, con `develop`, `release` y `hotfix`: sirve para mantener varias versiones en producción, que este proyecto no tiene, y agrega un paso de integración extra. Trunk-based sin pull requests: más rápido, pero sin revisión entre pares.

## Consecuencias

(+) Flujo corto de la rama a `main` y de ahí a las imágenes publicadas en GHCR. (+) Toda integración pasa por revisión y CI. (−) Sin rama de preproducción, `main` debe estar siempre sana, lo que exige un CI estricto (ADR-019).