# ADR-021: Disponibilidad: Clases y Reservas siguen funcionando con Pagos apagado

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #25, #40

## Contexto

El atributo de disponibilidad exige que el núcleo del gimnasio funcione aunque Pagos esté caído.

## Decisión

Clases no depende de Pagos. Con Pagos detenido, `GET /api/clases` y `POST /api/reservas` (con miembro activo) siguen respondiendo. Los errores de Miembros se manejan con timeout corto y 503 controlado. El procedimiento queda documentado como paso 6 de la demo.

## Alternativas consideradas

Llamadas síncronas entre Clases y Pagos (acoplan la disponibilidad).

## Consecuencias

El aislamiento es demostrable. Pagos y Miembros se recuperan procesando los eventos pendientes cuando vuelven.
