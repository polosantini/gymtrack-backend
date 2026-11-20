# Version 2: rechazo de pagos sin membresia en el backend

El ZIP V2 incluye todas las correcciones de V1 y resuelve el punto que quedaba pendiente: POST /api/pagos ya no registra un pago cuando el miembro no tiene una membresia creada.

## Resultado HTTP

| Situacion | Resultado | Pago guardado | Evento de pago |
| --- | --- | --- | --- |
| Miembro con membresia existente, incluso SUSPENDIDA o VENCIDA | 201 REGISTRADO | Si | pago.registrado despues del commit, con outbox |
| Miembro existente sin membresia | 409, El miembro no tiene una membresía creada | No | No |
| Miembro inexistente | 404, El miembro no existe | No | No |
| Miembros no responde, RabbitMQ falla o respuesta invalida | 503, No se pudo validar la membresía para registrar el pago, intente de nuevo | No | No |
| Entrada mal formada o plan/periodo/monto invalidos | 400 | No | No |

Las respuestas de error conservan timestamp, status, error, message y path. No se confia en tieneMembresia, estado o plan de una supuesta membresia enviados por el frontend. La fuente es la BD de Miembros. No se requiere que la membresia este ACTIVA: el primer pago o una renovacion deben poder activarla.

## Canal interno nuevo

Se incorpora una consulta AMQP request/reply, separada de los eventos de negocio:

- Exchange directo: gymtrack.consultas.
- Routing key: miembro.membresia.validar.pago.
- Cola atendida por miembros-service: miembros.validar.membresia.pago.rpc.
- Request de common/rpc: ConsultarMembresiaPago(miembroId).
- Response de common/rpc: RespuestaMembresiaPago(miembroId, existeMiembro, tieneMembresia).
- Timeout de respuesta predeterminado: 2500 ms. La solicitud tiene expiracion para no acumular consultas obsoletas.
- Propiedad gymtrack.pagos.validacion-timeout-ms: admite 100 a 5000 ms; predeterminado 2500.
- Conexion AMQP de Pagos limitada a 2 segundos.

La unica llamada REST entre servicios continua siendo Clases→Miembros /acceso. Esta extension agrega una dependencia de consulta de Pagos a Miembros por RabbitMQ; hay que incluirla en el diagrama y revision de arquitectura. Los JSON y routing keys de pago.registrado, reserva.cancelada, reserva.cupo.asignado y membresia.proxima.vencer se conservan.

Pagos depende de un puerto ValidadorMembresiaPago; la implementacion de infraestructura usa RabbitMQ. La comprobacion ocurre antes de construir/guardar Pago y de publicar el evento. La respuesta RPC solo valida existencia; la activacion sigue siendo asincrona y transaccional con idempotencia/outbox.

Actualizar y reconstruir miembros-service y pagos-service junto con common. Si Pagos nuevo se ejecuta frente a Miembros antiguo sin el consumidor de consulta, respondera 503 y no aceptara pagos. Esto es un fallo cerrado deliberado, no se omite la validacion al estar el otro servicio caido.

## Verificacion

Se agregaron pruebas de request/response JSON, consulta en Miembros, gateway AMQP, rechazo antes del repositorio, HTTP 409/404/503 con cero pagos y cero eventos, y admision de membresias suspendidas/vencidas. El script del check Docker prueba tambien el 409 real y que el historial quede vacio, incluso si el cliente manda tieneMembresia=true.

La validacion local usa H2 y RabbitMQ 4.0.5 reales fuera de Docker. Los checks del PR deben repetirla con el PostgreSQL y RabbitMQ del Compose. Si Rabbit falla despues de validar y durante la publicacion de un pago ya confirmado en BD, la outbox de V1 sigue conservando el evento; si falla antes de validar, la nueva solicitud devuelve 503 sin guardar.

## Actualizacion

El proyecto completo esta en gymtrack-backend. Si ya aplicaste V1, CAMBIOS_DESDE_V1.txt enumera lo nuevo y el parche exterior ACTUALIZAR_V1_A_V2.patch permite aplicarlo sobre esa version. Usar una de las dos vias y conservar .env y personalizaciones locales.

Commit sugerido:

fix(pagos): rechazar pagos sin membresia antes de guardar

Descripcion:

Validar existencia de membresia con Miembros mediante AMQP request/reply antes de persistir un pago. Responder 409 sin membresia, 404 si no existe el miembro y 503 si la validacion no esta disponible, sin guardar pago ni evento. Conservar 201 REGISTRADO y la activacion asincrona para membresias existentes. Mantener los contratos HTTP y los eventos de negocio originales.
