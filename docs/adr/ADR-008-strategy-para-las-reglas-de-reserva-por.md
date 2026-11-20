# ADR-008: Strategy para las reglas de reserva por plan, con DIP

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #23, #24, #39

## Contexto

Básico permite 2 reservas por semana, Full 1 por día y Premium es ilimitado. Un if/switch por plan en el caso de uso rompe OCP.

## Decisión

`ReglasReservaStrategy` con `ReglaBasico`, `ReglaFull` y `ReglaPremium`, y un `ReglasReservaResolver`. `ReservarClaseUseCase` recibe la estrategia por constructor y no importa reglas concretas. Cuentan CONFIRMADA y EN_ESPERA (no CANCELADA); la semana va de lunes a domingo; la regla se aplica antes del aforo.

## Alternativas consideradas

Condicionales por plan en el caso de uso (más simple, pero cada plan nuevo modifica código existente).

## Consecuencias

Un plan nuevo agrega una clase sin tocar el caso de uso, y el caso de uso se prueba con una estrategia falsa. A cambio hay más clases.
