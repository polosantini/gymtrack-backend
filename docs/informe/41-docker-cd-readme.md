# #41 — Dockerfile genérico, CD backend y README (GitHub #55)

Rama: `feature/41-docker-cd-readme`, desde `origin/main` en `51beb5e12359c3a0200962a8a033b42d771983cf`.

## Aplicar con GitHub Desktop

1. Seleccionar main, Fetch origin y Pull origin. Crear `feature/41-docker-cd-readme` desde main actualizado.
2. Extraer el ZIP en la raíz del repositorio conservando carpetas. Incluir `.github`, `.dockerignore` y `.env.example`.
3. Revisar cambios, hacer commit y publicar la rama. Abrir PR hacia main.
4. Esperar CI y Validar Docker y arranque. Publicación se omite intencionalmente en PR.
5. Después del merge, esperar las tres publicaciones de main. El propietario configura visibilidad pública si se desea descarga anónima.
6. Ejecutar el arranque del README en un equipo Docker y registrar resultados. Después continuar #40 usando este Compose.

## Commit

Summary:

```text
feat(infra): agregar Dockerfile generico y CD de backend a GHCR (#41)
```

Description:

```text
Agregar Dockerfile Java 21 parametrizado con SERVICIO para los tres servicios.
Configurar Compose con imagenes GHCR, variables e infraestructura existente.
Validar construccion y arranque HTTP tras CI y publicar solo desde main.
Documentar arranque, variables, persistencia y primera publicacion.

Refs #55
```

## PR

Título: `feat(infra): Dockerfile genérico, publicación GHCR y README (#41)`

Descripción:

```text
Permite construir Miembros, Clases y Pagos con un único Dockerfile y arrancar la demo desde imágenes GHCR. Compose mantiene PostgreSQL y RabbitMQ y configura la comunicación interna.

La publicación usa GITHUB_TOKEN y solo se ejecuta en main después de mvn verify y de construir/arrancar los tres servicios con comprobaciones HTTP. En PR se valida sin publicar. El namespace se convierte a minúsculas y se publican latest y sha del commit.

El README documenta requisitos, variables, arranque, persistencia y visibilidad inicial de paquetes.

Validación local: actionlint 1.7.12 sin errores, sintaxis YAML/Bash, resolución de Compose con valores predeterminados y personalizados, variables contrastadas con application.yml de los tres servicios, protección de .env y comprobaciones de estructura/dependencias. No se ejecutó Docker localmente porque el entorno no tiene daemon. La construcción y el arranque reales deben quedar verdes en el nuevo job. Tras el merge, verificar las tres publicaciones y docker compose pull && docker compose up -d antes de dar por cumplida la demo.

Refs #55
```

## Criterios y evidencia

| Criterio | Implementación | Evidencia pendiente |
| --- | --- | --- |
| Dockerfile raíz y ARG SERVICIO | Multietapa Java 21 para tres módulos | Tres construcciones en validar-docker |
| CD solo main y CI correcta | Dependencias ci/validar-docker y condición push/main | Jobs omitidos en PR y publicados en main |
| Tres imágenes GHCR | Matriz por servicio y namespace minúsculo | Tres paquetes y etiquetas del mismo SHA |
| Compose usa GHCR | Imágenes explícitas, sin build | Descarga real |
| pull y up levantan demo | Dependencias, variables y salud de infraestructura | Salida de comandos y respuestas HTTP |
| README y variables | README y .env.example | Seguir pasos en equipo de demo |
| Sin secretos adicionales | GITHUB_TOKEN y packages: write | Publicación en main |

La validación estática no equivale a una construcción Docker ni a una publicación. No se modifican Java, contratos, reglas de reserva ni POM. Esta configuración de infraestructura es compartida y debe revisarse con A.
