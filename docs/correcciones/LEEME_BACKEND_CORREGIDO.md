# Backend corregido: integridad, estados y eventos

Base exacta: gymtrack-backend-main (5).zip, revision f492e03e0cc70126e21a5c050a8f8e57c2e37109.
Version 2: incluye la validacion autoritativa ANTES de guardar un pago. Esta entrega contiene solo el backend. Los formatos de los endpoints existentes y los JSON/routing keys de eventos se conservan. Se añade el endpoint compatible PATCH /api/notificaciones/{id}/leer.

## Cambios aplicados

| Área | Corrección |
| --- | --- |
| Cuota Básico/Full | Candado persistente por miembro, adquirido antes del lock de clase y del conteo de Strategy; evita superar el límite entre clases distintas concurrentes. Premium sigue sin límite de plan. |
| Pool de conexiones | La fila de coordinación se prepara antes de iniciar la transacción de reserva. Evita que diez solicitudes retengan todas las conexiones mientras intentan crear otra transacción. El bloqueo que garantiza la cuota es de BD, no un synchronized local. |
| Cancelación | Reserva y evento pendiente se guardan en la misma transacción. Observer intenta publicar después del commit; una caída del broker no transforma una cancelación guardada en un falso HTTP 500. |
| Asignación de cupo | Mantiene lock, recibo idempotente y orden de espera; entrega el aviso a la outbox durable sin volver a promocionar al repetir la cancelación. |
| Validacion previa de pago | Pagos consulta a Miembros por AMQP request/reply antes de guardar. Sin membresia: 409; miembro inexistente: 404; timeout/fallo: 503. En los tres casos no guarda pago ni evento. Una membresia suspendida/vencida si puede pagarse. |
| Pagos y vencimientos | Outbox por servicio; los eventos se conservan en su propia BD y se publican después del commit con reintentos programados. |
| Entrega AMQP | Publisher confirms correlacionados y mandatory. Un nack, timeout o mensaje devuelto no marca la outbox como enviada. Si se confirma pero el proceso cae antes de guardar el resultado, puede haber redelivery; los consumidores deduplican. |
| Idempotencia de pagos | Tabla pago_aplicado por cada pago y lock del miembro en el consumidor. A-B-A no vuelve a extender la membresía; dos entregas simultáneas acreditan una sola vez. |
| Orden de planes | Un pago antiguo válido puede acreditar su período una vez, pero no revierte el plan establecido por un pago posterior. |
| Estado del evento | FALLIDO y PENDIENTE no activan ni renuevan; se validan identidad, plan/período y monto. El evento de esta aplicación sigue siendo REGISTRADO. |
| Estado de membresía | Los DTO devuelven VENCIDA si ACTIVA ya superó la fecha. /acceso sigue verificando estado y vigencia. No hay que esperar a un job para que la API indique que no tiene acceso. |
| Notificaciones | Identidad procesada persistente para pago, vencimiento y cupo; evita duplicados bajo redelivery. Un aviso de cupo sin messageId estable se rechaza para recuperación. |
| Fallos de consumidor | Reintentos acotados y republicación confirmada a colas de recuperación, sin cambiar los argumentos de las colas existentes. |
| Errores de entrada | Miembros y Pagos devuelven 400 en parámetros/JSON mal formados y conservan códigos HTTP del framework. Se valida la precisión admitida por la columna de monto. |
| Configuración | Corrige la ubicación YAML de gymtrack.vencimientos y spring.task.scheduling; habilita scheduling para las outbox. |
| Notificaciones leídas | PATCH /api/notificaciones/{id}/leer devuelve el mismo formato de NotificacionResponse con leida=true; repetirlo es idempotente. |
| Compose | Conserva los cinco servicios backend y datos, con healthchecks como cadenas y sangría convencional. El frontend se integra aparte por C. |
| CI | Conserva Maven y CD; añade script HTTP/AMQP de cuotas y pagos sobre los contenedores del check de Docker, y conserva evidencia JSON. |

## Arquitectura y criterios de aceptación

- ReglasReservaStrategy y sus implementaciones permanecen en domain/strategy; el caso de uso sigue recibiendo la interfaz, sin if/switch por plan.
- La coordinación por miembro es un puerto de application con implementación de persistencia. La nueva tabla candado_miembro guarda únicamente una clave de coordinación; no replica membresías ni planes.
- Se mantienen interfaces de consulta/reserva separadas y Observer en domain/observer.
- Se mantiene PESSIMISTIC_WRITE sobre la clase antes de contar aforo. El candado adicional protege la cuota global del miembro; ambos se mantienen hasta el commit de la reserva.
- EN_ESPERA cuenta para la cuota y CANCELADA no. La promoción no aumenta el número de reservas que cuentan para el plan, solo cambia EN_ESPERA por CONFIRMADA.
- La cancelación no asigna cupos directamente: persiste CANCELADA y el evento, Observer publica tras commit, el consumidor realiza la asignación.
- La única llamada REST entre servicios sigue siendo Clases→Miembros /acceso. Se añade una consulta privada AMQP request/reply Pagos→Miembros para validar existencia antes del pago; queda documentada en VALIDACION_PREVIA_PAGOS.md. No replica datos ni altera los eventos de negocio originales.
- common sigue conteniendo contratos y eventos; cada servicio mantiene su propia outbox y sus transacciones.

## Validación realizada

- Reactor completo mvn verify: 225 tests, 0 fallos, 0 errores, 0 omitidos.
- Cuotas: diez solicitudes concurrentes a clases diferentes del mismo miembro; Full admite 1, Básico 2, Premium 10. Full y Básico se repiten tres veces, con un pool de cinco conexiones para detectar agotamiento por transacciones anidadas.
- Pago A-B-A, entregas concurrentes, eventos fuera de orden, estados FALLIDO/PENDIENTE y estado vencido coherente.
- Cancelación HTTP con fallo AMQP: 200, CANCELADA persistida y evento pendiente; recuperación sin volver a cancelar; rollback sin evento; nack no marca enviado y ack sí.
- Pago con fallo de publicacion posterior a validacion: 201 REGISTRADO y outbox pendiente; recuperacion sin registrar otro pago. Si Rabbit/Miembros no permiten validar antes de guardar: 503 y no se registra nada. Rollback sin evento.
- Notificaciones duplicadas, cupo sin identidad y marcado de leída idempotente.
- JSON, parámetros y monto inválidos: 400.
- Actionlint, sintaxis Python y Docker Compose config correctos.
- Tres JVM reales, H2 y RabbitMQ 4.0.5: pago→activación→reserva, cuotas por HTTP, A-B-A-C y entrada inválida; apagado real del broker, cancelacion conservada y pago rechazado con 503 antes de guardar; recuperacion al restaurarlo, reintento de pago valido, promocion y una sola notificacion de cupo. Se comprobo pago sin membresia con 409 y cero registros/notificaciones, miembro inexistente con 404 y Miembros apagado con 503 en unos 2,5 segundos.

El entorno Java local recortado requirió source/target 21 sin --release y agente Mockito explícito; no se modificaron los POM. CI utiliza su JDK completo. Esta ejecución local no utilizó Docker ni PostgreSQL. El check Docker del PR ejecutará el nuevo script con PostgreSQL y RabbitMQ de Compose; debe quedar verde antes de fusionar.

## Aplicar con GitHub Desktop

1. Hacer copia de tu carpeta actual y conservar .env, datos y configuraciones locales.
2. Actualizar main y crear fix/backend-integridad-eventos desde ese main.
3. Extraer el ZIP. La carpeta gymtrack-backend contiene el proyecto completo. Para aplicar sobre el repositorio existente, copiar los archivos enumerados en CAMBIOS_APLICADOS.txt, conservando sus rutas.
4. No copiar target, node_modules ni .git desde otro proyecto. No hay .env con secretos en la entrega.
5. Si tu Compose local ya incluye frontend, conservar esa integración antes de reemplazar docker-compose.yml. El archivo incluido contiene los cinco servicios backend; C debe integrar su frontend con los nombres miembros/clases/pagos.
6. Hacer commit, publicar y abrir PR. Esta corrección incluye A y B: ambos deben revisar las partes que mantienen.
7. Esperar ci y Validar Docker y arranque. En un PR la publicación se omite; después del merge se publican los tres servicios en main.

Summary sugerido:

```text
fix(backend): asegurar cuotas, pagos idempotentes y eventos recuperables
```

Descripción sugerida:

```text
Evitar superar las cuotas de Básico/Full con reservas concurrentes en clases distintas.
Persistir eventos pendientes junto con el negocio y publicarlos tras commit con confirms y reintentos.
Deduplicar cada pago y notificación, proteger cambios de plan y rechazar estados de pago no admitidos.
Unificar errores de entrada 400 y el estado efectivo de membresías vencidas.
Validar por AMQP la existencia de membresia antes de guardar pagos, con rechazo 409/404/503 sin persistencia.
Agregar regresiones de concurrencia, rollback, broker caído, recuperación y contratos HTTP.
Mantener Strategy, DIP, ISP, Observer, routing keys y la única llamada REST entre servicios.
```

## Probar la versión nueva localmente

Al copiar código, los contenedores existentes siguen ejecutando la imagen anterior. Construir las tres imágenes desde la raíz del backend, con los mismos nombres que usa el Compose predeterminado:

```bash
docker build --build-arg SERVICIO=miembros-service -t ghcr.io/polosantini/gymtrack-backend/miembros-service:latest .
docker build --build-arg SERVICIO=clases-service -t ghcr.io/polosantini/gymtrack-backend/clases-service:latest .
docker build --build-arg SERVICIO=pagos-service -t ghcr.io/polosantini/gymtrack-backend/pagos-service:latest .
docker compose -f docker-compose.yml up -d --pull never --force-recreate miembros clases pagos
```

Conservar el volumen de Postgres y el contenedor RabbitMQ. Las tablas/columnas nuevas se crean con el ddl-auto=update ya existente; no hace falta down -v. Respaldar los datos antes de actualizar. Los nombres anteriores suponen GHCR_NAMESPACE e IMAGE_TAG predeterminados; si se personalizaron, usar los mismos valores al etiquetar.

Con los cinco servicios arriba y Python 3.9+:

```bash
python scripts/demo/probar-integridad-backend.py
```

En Git Bash de Windows, si python no está en PATH se puede usar `py -3 scripts/demo/probar-integridad-backend.py`. Los husos horarios usan zoneinfo; el runner Linux ya tiene los datos IANA. En Windows puede requerirse `py -3 -m pip install tzdata` si la distribución de Python no los trae.

Variables opcionales del script: MIEMBROS_URL, CLASES_URL, PAGOS_URL, RABBIT_MANAGEMENT_URL, RABBIT_USER, RABBIT_PASSWORD y EVIDENCIA_INTEGRIDAD. Los valores predeterminados usan localhost:8081/8082/8083/15672 y gymtrack/gymtrack. Las credenciales personalizadas de .env deben exportarse para este script Python, que no interpreta .env. El script crea fixtures de prueba identificados como Integridad y los conserva; ejecutarlo en la BD de demo, no sobre datos operativos.

La prueba de #40 continúa disponible:

```bash
powershell -NoProfile -File ./scripts/demo/probar-pagos-apagado.ps1
```

## Migración y recuperación de eventos

- Las tablas nuevas son evento_pendiente (en cada servicio), candado_miembro (Clases), pago_aplicado (Miembros) y evento_notificacion_procesado (Pagos). Se añade además la columna nullable pagos_previos_hasta a membresia. Se conservan los datos existentes.
- Antes de esta versión no había historial completo de pagos ya aplicados en Miembros. Para evitar acreditar a ciegas eventos antiguos no reconocidos, el consumidor establece un corte con ultimoPagoId. Un evento antiguo anterior a ese corte se conserva para revisión en recuperación. Un pago nuevo y único, incluso fuera de orden después del corte, se acredita una vez.
- La actualización no puede inferir ni corregir automáticamente fechas que ya se extendieron mal antes del arreglo. Revisar los casos antiguos con el historial de pagos y respaldar antes de ajustar fechas.
- Las colas de recuperación son gymtrack.miembros.recuperacion.queue, gymtrack.pagos.recuperacion.queue y gymtrack.clases.recuperacion.queue. Conservan el mensaje y contexto del fallo. No se añaden argumentos nuevos a las colas antiguas, evitando PRECONDITION_FAILED al actualizar un broker existente.
- Tras corregir la causa de un mensaje recuperado, revisar sus headers y republicarlo en gymtrack.events con la routing key original. Conservar payload e identidad; no crear un pago distinto para compensar automáticamente. Revisar el mensaje antes de reprocesarlo, especialmente los anteriores al corte de migración.
- Los reintentos de outbox se realizan cada 5 segundos en lotes de hasta 20 pendientes, con lock de fila. Los eventos pueden repetirse si un proceso cae después de que Rabbit confirme y antes de marcar enviado: los consumidores usan deduplicación durable.

## Coordinación pendiente del frontend (responsable C)

Esta entrega no modifica el frontend. Para el flujo normal de adquisición, C debe llamar primero POST /api/miembros/{id}/membresias y después POST /api/pagos. Un POST de pago sin membresia devuelve ahora 409 y no genera registro ni evento. La comprobacion es una consulta AMQP a Miembros antes de persistir; si no responde, devuelve 503. Una solicitud valida sigue respondiendo 201 REGISTRADO y activa/renueva por el evento de negocio asincrono despues del commit. Los eventos antiguos ya publicados sin membresia siguen pudiendo recuperarse mediante la cola de errores, pero las solicitudes nuevas se rechazan antes de generarlos.

C debe mostrar activación pendiente tras el 201 y consultar /acceso con polling acotado o Actualizar, evitar respuestas obsoletas al cambiar miembro/día y habilitar la reserva desde /acceso. El pago sigue REGISTRADO, no se inventa COMPLETADO para simular una pasarela. El nuevo PATCH /api/notificaciones/{id}/leer queda disponible para su botón de marcar leída.

No se añadieron reglas de monto igual a tarifa, prohibición de reservar fechas pasadas ni vigencia en la fecha futura de una reserva: requieren un acuerdo de negocio que no estaba en los criterios proporcionados. La validación de monto solo refleja positividad y precisión de la BD.
