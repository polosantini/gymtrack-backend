# ADR-002: Tres microservicios, cada uno con su propia base de datos

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #2, #9, #10, #14

## Contexto

El enunciado pide responsabilidades separadas, disponibilidad independiente y evidencia de consistencia por servicio.

## Decisión

Miembros (8081, `gymtrack_miembros`), Clases (8082, `gymtrack_clases`) y Pagos (8083, `gymtrack_pagos`). Una instancia de Postgres con una base por servicio, creadas en `docker/postgres/init.sql`.

## Alternativas consideradas

Monolito modular (más simple, pero no demuestra disponibilidad ni mensajería); una instancia de Postgres por servicio (más pesado para una demo local).

## Consecuencias

Bajo acoplamiento y la prueba de Pagos apagado es posible. No hay joins entre servicios (referencias lógicas por `miembroId`) y la consistencia entre servicios es eventual.
