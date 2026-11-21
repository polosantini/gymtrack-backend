# ADR-004: Exchange topic único `gymtrack.events` con una cola por consumidor

- **Estado:** Aceptada
- **Fecha:** 2026-11-05
- **Issues relacionados:** #2, #3, #30–#35

## Contexto

`pago.registrado` lo consumen dos servicios (Miembros activa la membresía y Pagos crea la notificación), y `reserva.cancelada` y `reserva.cupo.asignado` también cruzan servicios.

## Decisión

Un exchange topic `gymtrack.events`. Las routing keys y el nombre del exchange son constantes en `common`. Cada consumidor declara su propia cola.

## Alternativas consideradas

Una cola compartida (los consumidores competirían por los mensajes); un exchange por evento (más infraestructura sin beneficio claro).

## Consecuencias

Se puede agregar un consumidor sin tocar al publicador. Las routing keys y la estructura de los 4 eventos son un contrato compartido que solo cambia con acuerdo del equipo.
