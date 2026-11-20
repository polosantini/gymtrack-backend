# ADR-003: Comunicación híbrida: eventos con RabbitMQ y una única llamada REST (`/acceso`)

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #22, #25, #30–#35

## Contexto

Clases debe saber en el momento de reservar si la membresía está activa. Los demás flujos (pagos, vencimientos, cupos) no necesitan respuesta inmediata.

## Decisión

Eventos asíncronos para `pago.registrado`, `membresia.proxima.vencer`, `reserva.cancelada` y `reserva.cupo.asignado`. REST síncrono solo de Clases a Miembros (`GET /api/miembros/{id}/acceso`) con timeout corto.

## Alternativas consideradas

Todo por eventos con réplica local de membresías (más complejo y con datos desfasados); todo por REST (acopla la disponibilidad de los servicios).

## Consecuencias

No hay réplicas de datos. Si Miembros cae, Clases responde 503 controlado y no puede validar reservas nuevas.
