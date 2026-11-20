# ADR-016: Sin autorización real: selector "actuar como" solo como etiqueta visual

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #36

## Contexto

La demo necesita mostrar vistas de administrador y de miembro, pero implementar autenticación excede el alcance.

## Decisión

El frontend agrega los headers `X-Role` y `X-User-Id` desde el cliente común (se usa `fetch`, sin instalar Axios). Las pantallas cambian según el rol. No hay JWT ni validación en el backend.

## Alternativas consideradas

Spring Security con JWT (seguro, pero costoso en 30 días).

## Consecuencias

Demo clara y rápida. Es una limitación de seguridad que debe declararse en el informe: cualquiera puede enviar esos headers.
