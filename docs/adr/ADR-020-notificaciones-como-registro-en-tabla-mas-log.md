# ADR-020: Notificaciones como registro en tabla más log, sin correos reales

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #10, #12, #31, #32, #35, #36

## Contexto

Los flujos necesitan mostrar avisos en la interfaz, pero un proveedor de correo está fuera del alcance.

## Decisión

`Notificacion` (miembroId, tipo, mensaje, fecha, leida) en `gymtrack_pagos`. Tipos: CONFIRMACION_PAGO, VENCIMIENTO_PROXIMO y CUPO_LIBERADO. Se consultan con `GET /api/notificaciones?miembroId=` ordenadas por fecha descendente y se registran en el log.

## Alternativas consideradas

Envío real por correo o push (más integración sin valor para la nota).

## Consecuencias

Los flujos se ven en la interfaz de forma simple. No hay entrega real al usuario.
