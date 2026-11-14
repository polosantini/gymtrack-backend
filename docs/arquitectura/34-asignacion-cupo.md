# #34 — Consumidor de cancelación y asignación de cupo (GitHub #49)

## Rama: dependencia obligatoria de Observer

Esta entrega es incremental sobre `feature/33-observer-cancelacion`, commit `a4a351aaeafb6613cb94d84f4a06fd1b3ec609f0`, que contiene el Observer del issue #48. La base main consultada todavía no contenía Observer.

| Situación al crear la rama | Origen de feature/34-asignar-cupo |
|---|---|
| #33 sigue sin fusionarse | feature/33-observer-cancelacion, después de copiar su ZIP y hacer su commit |
| #33 ya está fusionado | main actualizado con Fetch origin y Pull origin |

No aplicar este ZIP directamente sobre un main que aún no contiene #33: faltaría su configuración AMQP y su publicador. Este ZIP solo incluye los cambios de #34, no vuelve a incluir todos los de #33. El PR debe indicar que depende de Observer; fusionar #33 antes de #34.

## Flujo y responsabilidades

1. `clases-service` declara su propia cola durable `gymtrack.clases.reserva.cancelada`, enlazada al exchange `gymtrack.events` con `reserva.cancelada`.
2. ConsumidorReservaCancelada recibe el record compartido de common y delega en AsignarCupoService.
3. `liberaCupo=false` termina inmediatamente: no modifica reservas, no guarda recibo ni publica aviso.
4. Para true, valida los campos y adquiere el mismo lock PESSIMISTIC_WRITE de Clase que las reservas nuevas y cancelaciones.
5. Comprueba la tabla CancelacionProcesada por reservaId antes de promover. Si ya se procesó, solo intenta terminar su publicación pendiente, si la hubiera.
6. Valida que la reserva original esté CANCELADA y corresponda a esa clase/fecha. Cuenta confirmadas y verifica que haya un cupo.
7. Busca exclusivamente EN_ESPERA de esa sesión, ordenadas por posicionEspera. Promueve la primera, elimina su posición y renumera las restantes como 1..N, conservando su orden.
8. Guarda en la misma transacción un recibo de la cancelación y una copia del evento de salida pendiente. Si no hay espera o la sesión ya está llena, guarda un recibo sin aviso.
9. Después del commit, PublicadorCupoPendiente abre una transacción nueva, bloquea ese recibo y publica solo si sigue pendiente. Marca el envío y confirma. Los reintentos concurrentes se serializan sobre el recibo.
10. Si el envío lanza una excepción, la asignación sigue confirmada, pero el aviso permanece pendiente. La entrega repetida de la cancelación vuelve a intentar ese mismo aviso; no promueve a otra persona.

El recibo usa reservaId como clave única: en este dominio una reserva solo puede cancelarse una vez. Volver a reservar tras cancelar crea otro id. No se deben reactivar a mano reservas canceladas con el mismo id para repetir la demo.

La tabla `cancelacion_procesada` se crea con el ddl-auto=update que ya utiliza clases-service. No se modifica la base de Miembros o Pagos ni el data.sql de demo.

## Contrato de salida sin cambios

Exchange: `gymtrack.events` (topic).

Routing key: **`reserva.cupo.asignado`**, con puntos.

```json
{"miembroId":4,"claseNombre":"Spinning","fecha":"2026-11-17","hora":"18:00"}
```

Se añade únicamente el metadato AMQP messageId estable, por ejemplo `reserva.cupo.asignado:1` para la cancelación de la reserva 1. No es un campo extra en el JSON. A puede usarlo para la idempotencia de su consumidor de Notificaciones (#50).

Una caída justo después de enviar a RabbitMQ y antes de confirmar la marca de envío puede causar una reentrega del mismo aviso. No se afirma entrega exactamente una vez entre base y broker. El mismo messageId identifica ese aviso repetido; #50 sigue siendo responsable de no duplicar la notificación. Tampoco se añaden publisher confirms ni una tarea programada para republicar: el reintento de avisos pendientes se realiza mediante la redelivery de la cancelación no confirmada por el listener.

## Errores y colas

| Cola / exchange | Función |
|---|---|
| gymtrack.clases.reserva.cancelada | Cola de negocio de Clases, durable |
| gymtrack.clases.eventos.dlx | Exchange local de errores |
| gymtrack.clases.reserva.cancelada.dlq | Conserva eventos inválidos rechazados sin requeue |

Un evento true con campos incompletos, reserva inexistente o sesión inconsistente se rechaza y acaba en la DLQ. Los errores transitorios se propagan para que el listener pueda reentregar el mensaje. No se vacían ni eliminan colas de negocio desde las pruebas Postman.

El consumidor está activo por defecto. Para detenerlo durante una prueba aislada del Observer #33, usar `--clases.eventos.reserva-cancelada.auto-startup=false`. En producción/demo normal debe mantenerse activo. Los tests de CI desactivan el listener mediante src/test/resources/application.properties; las pruebas AMQP reales se ejecutaron aparte con el listener activo.

## Criterios de aceptación y evidencia

| Criterio | Comprobación |
|---|---|
| Consume desde cola propia | Cola durable y binding exacto; consumo de JSON real publicado en RabbitMQ |
| false no hace nada | Test JPA sin cambios ni recibo; colección sin avisos ante false |
| Primera espera de la clase/fecha | Test con id fuera de orden, huecos, otra clase y otra fecha |
| Promueve y ajusta posiciones | Primera CONFIRMADA sin posición; restantes 1..N, únicamente al promover |
| Sin espera no publica | Recibo sin salida; duplicado posterior no asigna a una llegada nueva |
| Publica los cuatro campos del contrato | Conversión JSON heredada de #33; colección valida valores y formato |
| Idempotente | Recibo persistente; duplicado con otro cupo libre; 6 entregas concurrentes repetidas 3 veces; reinicio real de aplicación y 3 repeticiones sin nuevo aviso |
| Mismo lock que reservas nuevas | Asignación y una nueva reserva compiten en dos hilos: se respeta la cola sin sobreventa |
| Clase llena de demo | Fixture de Spinning con aforo 3, reservas 1/2/3 CONFIRMADA y 4 EN_ESPERA |

## Resultados de pruebas

- Reactor Maven completo: **148 tests, 0 fallos, 0 errores, 0 omitidos**. Common 5, Miembros 10, Clases 125, Pagos 8.
- Pruebas de rollback: no cambia la espera, no persiste recibo ni publica.
- Fallo simulado de envío: se conserva el aviso pendiente; reintento publica sin nueva promoción.
- Prueba real con Spring Boot, RabbitMQ 4.0.5 y H2 persistente en disco, sin mocks del transporte AMQP.
- Colección Postman ejecutada con Newman: **32 aserciones correctas**. Última ejecución: 19 peticiones HTTP, incluyendo sondeos asíncronos; el número de peticiones puede variar con los sondeos. La colección tiene 17 pasos.
- Se reinició Clases conservando la base; repetir 3 veces la cancelación original no produjo otro aviso ni otra asignación.
- Un evento inválido publicado en RabbitMQ se consumió y conservó en la DLQ, sin bucle de reentregas.
- Resumen de los eventos y resultados: docs/evidencias/asignacion-cupo/resumen-newman.json.

Límites: no se ejecutó la interfaz gráfica de Postman, sino su colección con Newman. No se ejecutó PostgreSQL ni el compose exacto del equipo; repetir allí usando rabbitmq:3-management antes de la demo. La creación de Notificacion en Pagos corresponde a A (#50) y no se presenta como probada en esta entrega.

El entorno de Java 21 está recortado: la ejecución Maven mantiene source/target 21, omite --release por falta de ct.sym y carga el agente de Mockito explícitamente. No se cambian los POM. En un JDK 21 completo usar `mvn -B verify`.

## Repetir Postman

1. Arrancar PostgreSQL, RabbitMQ y Clases con los cambios de #33 y #34. Comprobar que el listener esté activo y exista gymtrack.clases.reserva.cancelada.
2. Importar docs/postman/asignacion-cupo.postman_collection.json.
3. Preparar una sesión de prueba llena y exactamente una persona en espera, como Spinning del seed original. Se cancelarán DOS reservas confirmadas de esa sesión.
4. Configurar clasesUrl, rabbitUrl, credenciales, claseId, fecha, reservaConfirmada y otraConfirmada. Los valores por defecto 1 y 2 solo sirven si esas reservas originales continúan confirmadas. Revisarlos antes de ejecutar.
5. Collection Runner ejecuta 17 pasos: crea una cola propia de evidencia para el aviso, cancela la primera reserva, espera la promoción, comprueba el payload, inyecta duplicado/false, cancela otra confirmada con la cola vacía y elimina solo la cola de evidencia.
6. Deben pasar 32 aserciones. Los sondeos para esperar el consumo asíncrono pueden añadir peticiones al contador.
7. Para repetir, usar reservas de prueba nuevas. No cambiar una CANCELADA a CONFIRMADA con SQL: su recibo persistente identifica esa cancelación como ya procesada. No se necesita borrar volúmenes.

Las posiciones de los demás siguen estables cuando se consulta o se cancela. Este issue ajusta posiciones exclusivamente al asignar, tal como exige #49. Puede ejecutarse sin el consumidor de Pagos de A: la cola de evidencia recibe su propia copia y no compite con su futura cola de Notificaciones.

## Subir desde GitHub Desktop

1. Si #33 no se fusionó: seleccionar feature/33-observer-cancelacion y comprobar que su código está en un commit. Si ya se fusionó: seleccionar main, Fetch origin y Pull origin.
2. Crear feature/34-asignar-cupo desde esa base correcta.
3. Copiar las carpetas clases-service y docs de este ZIP en la raíz del repositorio, aceptando reemplazar los archivos de este paso.
4. Revisar Changes; no copiar de nuevo el ZIP antiguo de #33 encima del #34, pues sobrescribiría ReservasAmqpConfig y otros ajustes.
5. Commit, Publish branch y Create Pull Request hacia main. Indicar dependencia de #33 y pedir que se fusione primero. Mientras #33 siga abierto, el diff hacia main también puede mostrar los cambios heredados; no significa que haya que eliminarlos.
6. Esperar CI verde y aprobación. No se ha publicado la rama ni creado el PR desde aquí.

Summary / título:

```text
feat(clases): asignar cupos desde la cola de cancelaciones (#34)
```

Description:

```text
Closes #49
Depende del PR de Observer (#33 del título / issue #48).

Consume reserva.cancelada desde una cola durable propia de clases-service. Si liberaCupo=false no modifica nada. Si es true, bajo el mismo lock de Clase valida el cupo y promueve la primera EN_ESPERA de esa clase/fecha, ajustando las posiciones restantes.

Guarda un recibo persistente por cancelación y el aviso pendiente en la misma transacción. Publica reserva.cupo.asignado después del commit con miembroId, claseNombre, fecha y hora. Un duplicado no vuelve a promover ni repetir un envío completado; un fallo de envío permite reintentar el aviso pendiente. Sin espera no publica. Incluye messageId AMQP estable sin cambiar el JSON del contrato y DLQ para eventos inválidos.

Validación: 148 tests correctos; RabbitMQ real 4.0.5 + H2 persistente; colección Postman ejecutada con Newman con 32 aserciones correctas; reinicio y repetición del evento sin efectos adicionales. Pendiente repetir sobre PostgreSQL/rabbitmq:3-management del compose del equipo. La creación de la notificación corresponde al #50 de A.
```

En Closes se utiliza #49, no el #34 del título.
