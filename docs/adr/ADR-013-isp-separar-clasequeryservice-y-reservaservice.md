# ADR-013: ISP: separar ClaseQueryService y ReservaService

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #26

## Contexto

El catálogo no debería depender de la lógica de reservas.

## Decisión

`ClaseQueryService` solo con consultas y `ReservaService` con reservas y cancelaciones, cada uno con su implementación. `ClaseController` no importa `ReservaService`.

## Alternativas consideradas

Un único `ClaseService` con todo (más rápido de escribir, pero interfaz sobredimensionada).

## Consecuencias

Cada controlador conoce solo lo que usa y queda como evidencia de ISP en el informe. Hay más interfaces.
