# Descargas Maven durante la construccion Docker

Base: main bc64eac, con #41 y #40 integrados.
Rama propuesta: fix/docker-descargas-maven.

## Diagnostico

El registro adjunto falla antes de compilar Java, al resolver spring-boot-starter-parent:3.3.5 desde Maven Central. Este registro dice absent / Could not find artifact y no muestra un codigo HTTP 429. Un registro anterior si mostro 429. La descarga directa del POM consultada durante esta revision respondio HTTP 200 y devolvio un POM valido. No se puede atribuir con certeza ambos fallos a la misma causa; tampoco hay evidencia para cambiar la version de Spring Boot.

## Cambio

- Dockerfile: cache BuildKit del repositorio Maven en /root/.m2, con sharing=locked. Reutiliza dependencias entre las construcciones del mismo builder. No se asume que persista entre runners de Actions distintos.
- Hasta tres intentos Maven, con esperas de 15 y 30 segundos, exclusivamente cuando el registro contiene errores de resolucion de artefactos. -U fuerza a comprobar de nuevo los artefactos ausentes. Un error de compilacion falla inmediatamente; al agotarse los intentos se conserva el codigo de error.
- Workflow: max-parallel: 1 para la matriz de publicacion. Se siguen publicando las tres imagenes, solo en main y tras CI/validacion Docker correctas.

El parche conserva ARG SERVICIO, Java 21, Spring Boot 3.3.5, GITHUB_TOKEN, contratos y logica de negocio. No modifica docker-compose.yml, el frontend ni las configuraciones locales de la demo.

Estas medidas reducen solicitudes repetidas y toleran algunos fallos intermitentes. No garantizan superar una caida prolongada, un artefacto realmente inexistente ni un problema persistente de red o repositorio. Si siguen fallando los tres intentos, revisar la nueva salida completa y el acceso a Maven Central desde el runner.

## Validacion realizada

- Descarga directa del POM de Spring Boot desde Maven Central: HTTP 200.
- Actionlint sin errores y git diff --check correcto.
- Sintaxis POSIX sh comprobada.
- Logica de reintento ejecutada con comandos simulados: descarga fallida y recuperacion en segundo intento; fallo persistente termina en el tercero; error de compilacion termina en el primero. Se verificaron codigos de salida y esperas solicitadas, sin ejecutar esas esperas en la prueba.
- No se construyeron imagenes Docker localmente: este entorno no dispone de daemon. El PR debe pasar ci y Validar Docker y arranque antes de fusionarse. Tras el merge, revisar las tres publicaciones de main.
- No se repitieron los tests Java locales porque no cambia Java ni POM; la CI del PR ejecuta mvn verify como antes.

## Aplicacion

1. Crear la rama fix/docker-descargas-maven desde main actualizado.
2. Copiar los archivos del ZIP respetando las carpetas. El parche no reemplaza tu Compose local con el frontend.
3. En el commit incluir solamente los archivos de este parche; conservar aparte cualquier cambio local de Compose/frontend.
4. Publicar y abrir PR hacia main. Esperar ci y Validar Docker y arranque.
5. Tras merge, comprobar tambien la publicacion GHCR de los tres servicios.

Summary:

```text
fix(ci): reutilizar dependencias Maven y reintentar descargas Docker
```

Description:

```text
Agregar cache Maven de BuildKit compartida con bloqueo entre builds.
Reintentar hasta tres veces los fallos de resolucion con esperas acotadas.
Conservar fallo inmediato en errores de compilacion.
Publicar las tres imagenes secuencialmente para reducir descargas simultaneas.

No modifica versiones, POM, contratos, Compose ni logica de negocio.
Relacionado con #55.
```

Descripcion del PR:

```text
Las construcciones Docker fallaban al descargar el parent POM de Spring Boot desde Maven Central, antes de compilar los servicios. El artefacto existe y su descarga directa respondio HTTP 200 durante el diagnostico; el log mas reciente no permite afirmar que sea otro 429.

Este cambio reutiliza dependencias en el mismo builder, reintenta errores de resolucion hasta tres veces con espera y serializa la publicacion de las tres imagenes. Los errores de compilacion y los fallos persistentes siguen haciendo fallar el job.

Validacion: Actionlint, sintaxis sh y casos de recuperacion/agotamiento/error de compilacion con dobles de comandos correctos. Construccion y arranque reales pendientes del check Docker del PR. Se mantienen versiones, contratos, CI previa y restricciones de publicacion en main.

Relacionado con #55.
```
