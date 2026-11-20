# ADR-017: Esquema con `ddl-auto=update` y datos de demo con `data.sql`

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #9, #10, #13, #14, #18

## Contexto

Se necesita levantar el entorno con datos sin pasos manuales y sin tiempo para gestionar migraciones.

## Decisión

Tablas generadas por Hibernate (`ddl-auto=update`, sin Flyway). Cada servicio carga su `data.sql` con IDs fijos y las secuencias se avanzan con `setval`. Clases refresca las filas de demo en cada arranque con fechas ancladas al próximo martes. `docker compose down -v && up -d` deja la base limpia.

## Alternativas consideradas

Flyway (versiona el esquema, pero añade trabajo y fricción entre 3 servicios).

## Consecuencias

Arranque rápido y demo reproducible. No hay historial de migraciones y el esquema podría desviarse de lo esperado en producción.
