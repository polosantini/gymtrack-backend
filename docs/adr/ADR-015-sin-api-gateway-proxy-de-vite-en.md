# ADR-015: Sin API Gateway: proxy de Vite en desarrollo y nginx en producción

- **Estado:** Aceptada
- **Fecha:** 2026-11-20
- **Issues relacionados:** #6, #42

## Contexto

Hay 3 servicios en puertos distintos y poco tiempo. Un gateway añade un componente más que mantener y justificar.

## Decisión

Proxy de Vite hacia los puertos 8081, 8082 y 8083 en desarrollo. En producción, `nginx.conf` con `proxy_pass` a los 3 servicios dentro de la imagen del frontend.

## Alternativas consideradas

Spring Cloud Gateway (más fiel a una arquitectura real, pero fuera del alcance).

## Consecuencias

Menos piezas y despliegue simple. No hay punto único para autenticación, rate limiting ni observabilidad.
