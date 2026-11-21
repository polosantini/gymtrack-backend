# ADR-005: Eventos publicados después del commit y consumidores idempotentes

- **Estado:** Aceptada
- **Fecha:** 2026-11-05
- **Issues relacionados:** #30, #31, #33, #34, #35, #37

## Contexto

RabbitMQ puede entregar un mensaje más de una vez, y un evento publicado antes del commit puede anunciar algo que luego se revierte.

## Decisión

Publicar solo después del commit de la transacción. Todos los consumidores (`pago.registrado`, `reserva.cancelada`, `reserva.cupo.asignado`) son idempotentes.

## Alternativas consideradas

Patrón Outbox (más robusto, pero fuera del alcance de 30 días); publicar dentro de la transacción (riesgo de evento fantasma).

## Consecuencias

Implementación simple y sin efectos duplicados. Queda un riesgo conocido: si el servicio cae entre el commit y la publicación, el evento se pierde. Se acepta y se documenta en el informe.
