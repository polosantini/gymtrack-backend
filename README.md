# GymTrack Backend

Backend Java 21 / Spring Boot: Miembros, Clases y Pagos, PostgreSQL con una base por servicio y RabbitMQ. Clases consulta el acceso en Miembros por REST y no depende de Pagos.

## Arrancar la demo

Requisitos: Git, Docker Engine con Compose v2 o Docker Desktop en modo contenedores Linux. Las imágenes de este workflow son Linux AMD64 (Intel/AMD).

Antes del primer arranque, deben haber terminado las **tres publicaciones** de imágenes en Actions sobre `main`. Un PR construye y valida; no publica. Tras la primera publicación, el propietario debe hacer públicos los tres paquetes en GitHub → Package settings → Change visibility para permitir descargas anónimas. Si se mantienen privados, el equipo deberá autenticarse para descargarlos. El CD utiliza el `GITHUB_TOKEN` automático y no requiere secretos adicionales.

```bash
git clone https://github.com/polosantini/gymtrack-backend.git
cd gymtrack-backend
docker compose pull && docker compose up -d
docker compose ps -a
```

Los valores predeterminados permiten arrancar sin `.env`. Para personalizarlos, copiar `.env.example` como `.env` antes del arranque: `cp .env.example .env` en Bash o `Copy-Item .env.example .env` en PowerShell. No subir `.env` al repositorio.

| Componente | Dirección predeterminada |
| --- | --- |
| Miembros | http://localhost:8081/api/miembros |
| Clases | http://localhost:8082/api/clases |
| Pagos | http://localhost:8083/api/pagos?miembroId=6 |
| RabbitMQ administración | http://localhost:15672 (gymtrack / gymtrack) |
| PostgreSQL | localhost:5432 |

`up -d` inicia los contenedores; esperar a que las aplicaciones terminen de arrancar. PostgreSQL y RabbitMQ tienen comprobaciones de salud. Diagnóstico y comprobaciones HTTP:

```bash
docker compose logs --tail 100 miembros clases pagos
curl -f http://localhost:8081/api/miembros
curl -f http://localhost:8082/api/clases
curl -f "http://localhost:8083/api/pagos?miembroId=6"
```

Si aparece `denied` al descargar, revisar visibilidad y permisos del paquete. Si no existe `latest`, revisar las tres publicaciones. Si un puerto está ocupado, ajustar su variable en `.env`.

## Variables

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `GHCR_NAMESPACE` | `polosantini/gymtrack-backend` | Propietario/repositorio en minúsculas |
| `IMAGE_TAG` | `latest` | Misma versión de los tres servicios |
| `DB_USER` / `DB_PASSWORD` | `gymtrack` / `gymtrack` | PostgreSQL y aplicaciones |
| `RABBIT_USER` / `RABBIT_PASSWORD` | `gymtrack` / `gymtrack` | RabbitMQ y aplicaciones |
| `POSTGRES_PORT` | `5432` | PostgreSQL en el equipo |
| `RABBITMQ_AMQP_PORT` | `5672` | AMQP en el equipo |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` | Consola RabbitMQ |
| `MIEMBROS_PORT` | `8081` | HTTP Miembros en el equipo |
| `CLASES_PORT` | `8082` | HTTP Clases en el equipo |
| `PAGOS_PORT` | `8083` | HTTP Pagos en el equipo |

Compose configura los nombres internos `postgres`, `rabbitmq` y `miembros`; dentro de un contenedor, `localhost` apunta al propio contenedor. Clases recibe `MIEMBROS_BASE_URL=http://miembros:8081`. Las aplicaciones reciben `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER` y `RABBIT_PASSWORD` para conectar con RabbitMQ.

Para reproducir una publicación concreta, establecer `IMAGE_TAG=sha-<SHA completo del commit de main publicado>`. Se publica también `latest`; esperar a que terminen los tres jobs antes de usarlo para evitar mezclar versiones durante la publicación. En un fork, ajustar `GHCR_NAMESPACE` al propietario/repositorio del fork en minúsculas.

Las credenciales predeterminadas son para la demo local; personalizarlas antes de exponer la infraestructura a otros equipos.

## Persistencia y parada

`docker/postgres/init.sql` crea `gymtrack_miembros`, `gymtrack_clases` y `gymtrack_pagos` al inicializar un volumen vacío. PostgreSQL conserva sus datos en `postgres-data`.

```bash
docker compose stop
docker compose start
# Eliminar contenedores y red conservando datos:
docker compose down
```

No utilizar `docker compose down -v` como parada normal: elimina los datos. Cambiar credenciales en `.env` no cambia automáticamente usuarios ya creados en un volumen existente. El script de inicialización tampoco vuelve a ejecutarse sobre ese volumen.

## Construcción y CI/CD

El Dockerfile raíz recibe `ARG SERVICIO`, compila el módulo seleccionado junto con `common` y copia el JAR ejecutable a Java 21 JRE, con usuario sin privilegios. Valores: `miembros-service`, `clases-service` y `pagos-service`.

Ejemplo de construcción manual desde la raíz:

```bash
docker build --build-arg SERVICIO=clases-service -t gymtrack-clases:local .
```

La imagen manual sirve para desarrollo. El Compose oficial usa imágenes publicadas en GHCR.

El workflow `.github/workflows/ci.yml` ejecuta:

1. `ci`: `mvn -B -U "-Dmaven.repo.local=${RUNNER_TEMP}/m2-gymtrack" verify` en PR y push a `main`.
2. `validar-docker`: después de CI correcta, valida Compose, construye los tres servicios sin publicar, levanta la demo y comprueba los tres endpoints HTTP. Muestra logs si falla y limpia los contenedores al terminar.
3. `publicar-imagenes`: solo en push a `main`, después de ambos jobs correctos, publica una imagen por servicio con `latest` y `sha-<SHA completo>`. Usa `GITHUB_TOKEN`, permiso `packages: write` y namespace convertido a minúsculas.

Los tests se ejecutan en CI; la construcción de imágenes omite repetirlos. No se requiere PAT ni secretos nuevos para el workflow. Publicar en GHCR no despliega en la nube.

## Siguiente prueba: #40, Pagos apagado

Con la demo arrancada, la prueba de disponibilidad usará este mismo Compose:

```bash
docker compose stop pagos
# Comprobar catálogo y una reserva válida con miembro activo.
docker compose start pagos
```

Mantener Miembros, PostgreSQL y RabbitMQ encendidos. La prueba completa está en [la guía de #40](docs/informe/40-pagos-apagado.md). Con los cinco servicios arrancados, ejecutar desde la raíz:

```powershell
pwsh -NoProfile -File ./scripts/demo/probar-pagos-apagado.ps1
```

En Windows también se puede utilizar `powershell -NoProfile -File .\scripts\demo\probar-pagos-apagado.ps1`. El script detecta los puertos publicados, requiere un miembro Premium ya activo (por defecto María, id 6), crea una clase nueva, detiene Pagos, comprueba catálogo/reserva/error controlado y restaura Pagos al finalizar. Conserva la clase y la reserva como evidencia; no modifica membresías ni elimina datos.

El job `Validar Docker y arranque` ejecuta esta prueba después del arranque HTTP y conserva un artefacto JSON. Una ejecución con `resultado=OK` es la evidencia de Docker; agregar el procedimiento no equivale a haberlo ejecutado correctamente.

Referencia: [publicación y visibilidad de paquetes GHCR](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).
