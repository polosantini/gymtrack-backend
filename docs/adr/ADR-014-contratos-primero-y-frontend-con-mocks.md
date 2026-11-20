# ADR-014: Contratos primero y frontend con mocks

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #6, #8, #19, #20, #36

## Contexto

C no puede esperar al backend, y A y B deben saber exactamente qué se envían.

## Decisión

Antes de escribir lógica se aprueban los endpoints y los 4 eventos (formato de fecha y zona horaria, planes, mensajes de error). El frontend usa mocks con el shape del contrato, y `VITE_USAR_MOCK` alterna mocks y API real sin tocar código. Los cambios posteriores se anotan en el hilo del #8 el mismo día. La Postman compartida es la evidencia de cumplimiento.

## Alternativas consideradas

Desarrollar el backend primero y generar el frontend después (bloquea a C).

## Consecuencias

Se trabaja en paralelo. El costo es mantener los mocks sincronizados con el contrato.
