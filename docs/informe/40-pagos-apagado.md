# #40 — Probar con Pagos apagado (GitHub #54)

Base: main `e20f5de`, con #41 ya integrado. Rama propuesta: `test/40-pagos-apagado`, creada desde main actualizado. Se utiliza el Dockerfile y el docker-compose.yml oficiales de #41, sin archivos Compose adicionales ni cambios a los servicios de producción.

## Objetivo y alcance

Demostrar que Clases mantiene catálogo y reservas aunque Pagos esté detenido. Se mantiene funcionando Miembros (autoriza el acceso), PostgreSQL y RabbitMQ. No se cambian membresías ni reglas para conseguir que la prueba pase.

| Criterio de aceptación | Comprobación |
| --- | --- |
| docker compose stop pagos deja Pagos fuera | Pagos responde antes; luego no está running, Docker informa exited y su HTTP deja de responder |
| GET /api/clases sigue respondiendo | HTTP 200 después de apagar Pagos y después del error controlado |
| POST /api/reservas funciona con miembro activo | HTTP 201 CONFIRMADA; miembro/clase/fecha coinciden; aparece en historial y quedan 9 de 10 cupos |
| Error controlado sin romper Clases | Petición sin miembro produce 400 con timestamp/status/error/message/path, sin trace; catálogo sigue en 200 |
| Procedimiento documentado | Este documento, script PowerShell y colección Postman |

Una membresía inactiva debe seguir rechazándose. El script requiere un Premium ya activo para aislar la disponibilidad de los límites de planes; el predeterminado es María, id 6. Cada ejecución crea una clase nueva con aforo 10, todos los días y fecha futura en America/Bogota, evitando duplicados y datos de demo ya ocupados. Conserva clase y reserva como evidencia y restaura Pagos incluso si una comprobación falla. No borra datos ni registra pagos.

## Preparación con Docker Desktop

1. Tener #41 integrado, Docker iniciado en modo contenedores Linux y las tres imágenes GHCR publicadas/accesibles. Esperar todas las publicaciones antes de usar latest; opcionalmente fijar IMAGE_TAG al mismo SHA de main publicado para los tres servicios.
2. Abrir PowerShell en la raíz del repositorio actualizado. Si se requieren puertos personalizados, usar `.env` según el README.
3. Arrancar la demo oficial:

```powershell
docker compose pull
docker compose up -d
docker compose ps -a
```

Deben estar arrancados postgres, rabbitmq, miembros, clases y pagos. La primera descarga puede tardar. No usar down -v: borraría datos. Si los datos persistentes de María ya no indican activa=true y plan=PREMIUM, elegir otro Premium activo mediante el parámetro MiembroId; el script no reactiva a nadie.

## Ejecución automatizada local

Desde la raíz, con PowerShell 7:

```powershell
pwsh -NoProfile -File ./scripts/demo/probar-pagos-apagado.ps1
```

En Windows también se puede ejecutar con Windows PowerShell:

```powershell
powershell -NoProfile -File .\scripts\demo\probar-pagos-apagado.ps1
```

La sintaxis se comprueba con PowerShell 7; la ejecución en Windows PowerShell 5.1 queda pendiente de verificación en ese equipo. El script usa el Compose raíz explícitamente y detecta los puertos publicados por Docker. Se puede elegir otro miembro, fecha o archivo de salida:

```powershell
pwsh -NoProfile -File ./scripts/demo/probar-pagos-apagado.ps1 -MiembroId 6 -Fecha 2026-11-24 -Salida ./evidencia-40.json
```

La fecha del ejemplo se puede cambiar por una fecha futura. Para instalaciones con URLs especiales se admiten ClasesUrl, MiembrosUrl y PagosUrl; deben corresponder a los contenedores de este Compose. El script espera HTTP 200 antes de empezar, verifica que el resto de servicios continúa arriba y restaura Pagos al terminar.

Salida predeterminada:

```text
docs/evidencias/40-pagos-apagado/ejecucion-docker.json
```

Solo sirve para cerrar #54 si indica resultado=OK, pagosEstadoContenedor=exited, pagosContenedorDetenido=true, pagosHttpInaccesible=true, pagosDetenidoDurantePrueba=true, reserva HTTP 201 y restauracionPagos="HTTP 200 tras start". Un FALLO debe investigarse con el error registrado y `docker compose logs --tail 100 miembros clases pagos`.

El JSON registra tiempos, commit, referencias de imágenes, URLs, clase/reserva y respuestas HTTP. El directorio generado está ignorado por Git; guardar la evidencia con el informe o descargar el artefacto de Actions, sin subir resultados ficticios al repositorio.

## Ejecución en GitHub Actions

El workflow CI backend conserva su job Maven y amplía `Validar Docker y arranque`:

1. Construye los tres servicios con el Dockerfile genérico del repositorio y etiquetas locales del commit en prueba. Así valida el código exacto del PR sin descargar imágenes de backend privadas ni publicar durante un PR.
2. Levanta el Compose oficial con las imágenes locales y comprueba los endpoints.
3. Ejecuta el script: detiene únicamente Pagos, verifica catálogo/reserva/error y restaura Pagos.
4. Conserva el JSON en el artefacto `evidencia-40-pagos-apagado-<run_id>-<run_attempt>` durante 30 días. Si el arranque previo falla, puede no existir JSON: no equivale a prueba aprobada.
5. Ante fallos, muestra logs; al terminar limpia los contenedores del runner. No toca los contenedores del equipo del usuario.

La publicación de GHCR sigue limitada a main después de los jobs correctos, ahora incluyendo esta comprobación de disponibilidad. Se utiliza GITHUB_TOKEN; no se añaden secretos ni un segundo workflow de despliegue.

En el PR comprobar tanto ci como Validar Docker y arranque. Descargar el artefacto desde Actions y revisar resultado=OK; que solo ci esté verde no demuestra el criterio Docker. Conservar el JSON fuera de Actions antes de que expire y enlazar la ejecución en el informe.

## Procedimiento manual del paso 6 de la demo

1. Mostrar `docker compose ps` con Pagos funcionando. Comprobar en Postman GET http://localhost:8083/api/pagos?miembroId=6 → 200 y GET http://localhost:8081/api/miembros/6/acceso → activa=true y plan=PREMIUM.
2. En la terminal ejecutar:

```powershell
docker compose stop pagos
docker compose ps -a pagos
```

3. Mostrar Pagos detenido y el resto en ejecución. Una petición al puerto de Pagos debe fallar por conexión; esto es esperado para el servicio apagado.
4. Importar `docs/postman/pagos-apagado.postman_collection.json` y ejecutarla con Collection Runner en orden. Ajustar variables de URL si hay puertos personalizados y miembroId si corresponde. La colección crea una clase nueva y reserva, comprueba historial, provoca un 400 controlado y vuelve a consultar cupos. No activar miembros manualmente para eludir las validaciones.
5. Esperado: siete peticiones y trece aserciones correctas. La colección no detiene el contenedor: el paso 2 y la comprobación de estado son obligatorios. Tampoco incluye una petición al puerto apagado, porque ese fallo de transporte esperado aparecería como error de colección.
6. Conservar evidencia de estado, respuestas y ejecución. Restaurar Pagos siempre, incluso si alguna petición falla:

```powershell
docker compose start pagos
docker compose ps
```

Esperar de nuevo HTTP 200 en Pagos. Usar los puertos configurados en .env si se cambiaron los predeterminados.

## Pruebas complementarias y estado de validación

`DisponibilidadSinPagosHttpTest` inicia Clases real con H2 y un servidor HTTP de Miembros simulado, sin iniciar pagos-service. Comprueba: reserva confirmada y catálogo disponible, membresía inactiva con 409 y fallo de Miembros con 503 controlado sin romper el catálogo. Usa una fecha fija y no altera la lógica de producción. No sustituye la ejecución Docker con PostgreSQL/RabbitMQ.

Validación local realizada:

- Reactor completo: **186 tests, 0 fallos, 0 errores y 0 omitidos**, incluidos los tres casos HTTP nuevos. El entorno Java 21 local recortado necesitó compilar con source/target sin --release y cargar explícitamente el agente Mockito; no se cambian los POM. Actions ejecuta el comando Maven normal con su JDK completo.
- Actionlint, parser PowerShell, resolución Compose y formato de los cambios correctos.
- Flujo PowerShell comprobado con dobles HTTP/Docker: caso correcto, fallo después de detener Pagos con restauración garantizada y rechazo de miembro inactivo antes de detenerlo. Esto comprueba el script y **no equivale a ejecutar contenedores**; sus salidas simuladas no se incluyen como evidencia de demo.
- Colección Postman validada como JSON; su ejecución con el backend Docker queda en el procedimiento manual.

Este entorno no tiene daemon Docker. **La ejecución de docker compose stop pagos sobre contenedores debe quedar verde en Actions o en Docker Desktop antes de cerrar #54.** No se incluye un JSON Docker prefabricado.

## Aplicar y abrir el PR

1. En GitHub Desktop: main → Fetch origin → Pull origin, con #41 ya fusionado.
2. Crear `test/40-pagos-apagado` desde ese main. Si ya existe una rama antigua de #40, usar una nueva desde el main actualizado; no mezclar la entrega anterior con Compose auxiliar.
3. Extraer este ZIP en la raíz conservando carpetas. Incluye el workflow, README, .gitignore, test Java, colección, script y esta guía; no reemplaza Dockerfile ni docker-compose.yml.
4. Hacer commit, publicar y abrir PR hacia main. Esperar ambos checks y revisar el artefacto.

Summary del commit:

```text
test(clases): comprobar reservas con Pagos apagado (#40)
```

Description del commit:

```text
Probar disponibilidad con el Compose oficial de #41.
Detener Pagos, verificar catalogo/reserva/error controlado y restaurarlo.
Conservar evidencia JSON en Actions y documentar el paso 6 de la demo.
Agregar pruebas HTTP de Clases sin pagos-service y coleccion Postman.

Refs #54
```

Título del PR: `test(clases): catálogo y reservas con Pagos apagado (#40)`

Descripción del PR:

```text
Demuestra disponibilidad de Clases cuando Pagos está detenido usando el Dockerfile y Compose oficiales de #41. El script verifica estado real del contenedor y fallo de conexión a Pagos, catálogo 200, reserva CONFIRMADA 201 persistida, cupos y error 400 controlado con catálogo todavía disponible. Restaura Pagos incluso ante fallos.

CI ejecuta la prueba después del arranque Docker y conserva un artefacto JSON. Incluye procedimiento de demo, colección Postman y pruebas HTTP de Clases sin pagos-service, con rechazo de membresía inactiva y fallo de Miembros controlado. No modifica lógica de producción, contratos, membresías ni datos de demo.

La prueba Docker debe quedar verde y el artefacto indicar resultado=OK antes de cerrar el issue. La validación local de tests/configuración no sustituye esa ejecución.

Refs #54
```

Después de comprobar la ejecución Docker y guardar la evidencia, cambiar Refs #54 por Closes #54 en el PR. #40 es el número del título y #54 el número real de GitHub.
