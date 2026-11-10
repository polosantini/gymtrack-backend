# Consistencia del aforo (GitHub #39)

`ClaseRepository.findByIdForUpdate` usa @Lock(PESSIMISTIC_WRITE). El caso de uso lo invoca antes del conteo de confirmadas. ReservaServiceImpl inicia la transacción: lock, comprobación de duplicados, Strategy, conteos, decisión e INSERT ocurren dentro de ella; el lock se libera al finalizar.

La prueba de concurrencia ejecuta 8 hilos con barrera de inicio y 1 cupo, repitiendo 3 veces: debe quedar 1 CONFIRMADA y 7 EN_ESPERA, con posiciones distintas. Usa transacciones reales e H2, no un repositorio simulado. La prueba formal de Fase 4 debe repetir la carga en PostgreSQL del despliegue.

Trade-off: el documento bloquea la fila Clase, que es una plantilla semanal. Por tanto, serializa TODAS las reservas de esa clase, incluso si se solicitan fechas distintas. Los conteos siguen siendo por (clase, fecha). Esto prioriza consistencia sobre concurrencia; una entidad Sesion permitiría bloqueo más fino en el futuro. No se añade en este alcance.

Este lock no serializa simultáneamente reservas del mismo miembro en clases diferentes: la garantía comprobada aquí es el aforo por clase/fecha. Reforzar límites de plan frente a ese escenario requiere un diseño adicional por miembro.
