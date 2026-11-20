# #38 — Prueba de concurrencia (GitHub #52)

## Base y rama

Crear `test/38-concurrencia` desde main actualizado. Base de esta entrega: `9acf3aa4a54b68bb8a6c3eeffc4c23480e33fe83`, idéntica al ZIP gymtrack-backend-main (4).zip recibido. Main ya incluye lock, lista de espera, Observer y consumidor. Este cambio no depende de los nuevos PR #39 y #40: sus archivos son distintos.

## Evidencia de consistencia

Se refuerza la prueba existente `ReservaConcurrenciaTest`, sin duplicarla. Arranca 10 hilos y usa CyclicBarrier para liberarlos juntos. Cada hilo solicita el único cupo de la misma clase/fecha para un miembro distinto. El servicio y los repositorios son reales con transacciones JPA/H2; solo se simula la consulta de Miembros para devolver PREMIUM activo y aislar el aforo de los límites de plan.

Se repite 5 veces con datos nuevos en cada repetición (50 solicitudes en total).

| Comprobación en cada repetición | Resultado esperado y obtenido |
|---|---|
| Cupos iniciales | 1 |
| Hilos | 10 |
| CONFIRMADA | 1 |
| EN_ESPERA | 9 |
| Posiciones únicas persistidas | 1 a 9, sin empates |
| Reservas persistidas | 10 |
| Miembros distintos en respuestas | 10 |

La fecha es fija: 2026-11-17. La prueba no depende del reloj o del día actual. El lock sigue en ClaseRepository.findByIdForUpdate y la reserva mantiene la transacción hasta guardar.

## Control negativo

En una copia de trabajo AISLADA se ejecutó la misma prueba retirando únicamente @Lock(PESSIMISTIC_WRITE). Las 5 repeticiones fallaron porque quedaron respectivamente 10, 7, 2, 4 y 4 confirmadas donde se esperaba 1. Esto comprueba que la prueba detecta sobreventa y no pasa por casualidad con un repositorio simulado.

El control negativo no forma parte del código entregado ni del CI. El lock del proyecto real no se eliminó. La copia de control se restauró después de registrar el resultado.

## Criterios de aceptación

- Varios hilos por el último cupo: 10, con CyclicBarrier.
- Solo uno confirmado y el resto en espera: comprobado en respuestas y base.
- Posiciones únicas: comprobadas y ordenadas de 1 a 9.
- Estable en repeticiones: 5 rondas correctas.
- Maven en verde: reactor completo con 176 tests, 0 fallos, errores u omitidos en esta rama aislada.
- Número de hilos, cupos y resultado documentados aquí para el informe.

El motor probado fue H2. No se presenta como carga ejecutada sobre PostgreSQL de producción. El entorno Java 21 recortado requirió omitir --release conservando source/target 21 y agente Mockito explícito; no se cambian POM. Con JDK 21 completo usar `mvn -B test`.

## Repetir

Desde la raíz del repositorio:

```text
mvn -B -pl clases-service -am -Dtest=ReservaConcurrenciaTest -Dsurefire.failIfNoSpecifiedTests=false test
```

O ejecutar `mvn -B test` para el reactor completo. Resumen de la ejecución en docs/evidencias/38-validacion/resumen-tests.json.

## GitHub Desktop

1. Main → Fetch origin → Pull origin.
2. Crear test/38-concurrencia.
3. Copiar las carpetas clases-service y docs del ZIP. Revisar que el cambio funcional es solo la prueba de concurrencia.
4. Commit y PR hacia main; esperar CI verde y revisión de A.

Summary:

```text
test(clases): verificar concurrencia con diez hilos (#38)
```

Description:

```text
Closes #52

Refuerza ReservaConcurrenciaTest con 10 hilos que compiten por 1 cupo y 5 repeticiones. Verifica 1 CONFIRMADA, 9 EN_ESPERA, posiciones persistidas únicas 1..9 y 10 reservas guardadas.

CyclicBarrier sincroniza el inicio. Mantiene fecha fija y usa JPA/H2 real, con la consulta de membresía simulada para aislar el aforo.

Incluye evidencia para el informe. El control negativo aislado sin lock produjo sobreventa y fallo en las 5 rondas; ese cambio negativo no se entrega.

Validación: mvn test del reactor completo en verde (176 tests en esta rama aislada).
```

El número del título es #38; en Closes va #52.

## Comprobación conjunta de las tres entregas

Se combinaron únicamente los cambios de pruebas de #38, #39 y #40 sobre la misma base en una copia aislada. El reactor completo pasó **186 tests, sin fallos, errores ni omitidos**. Esto comprueba que las tres entregas pueden coexistir; no sustituye la prueba Docker pendiente de #40.
