# ADR-018: Docker Compose, Dockerfile genérico y CD a GHCR

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #2, #41, #42

## Contexto

La demo debe levantar desde imágenes y no desde el código fuente.

## Decisión

`docker-compose.yml` con Postgres y RabbitMQ (management en 15672). Un Dockerfile en la raíz con `ARG SERVICIO` para los 3 servicios. El CD publica una imagen por servicio en GHCR solo en `main` y con la CI en verde. El frontend usa un Dockerfile multi-stage (node para compilar y nginx para servir).

## Alternativas consideradas

Un Dockerfile por servicio (duplicación) o ejecutar con Maven en la demo (lento y frágil).

## Consecuencias

Demo reproducible con `docker compose pull && up -d`. Depende de GHCR y de la CI.
