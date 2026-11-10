# Lista de espera estable (GitHub #40)

Una reserva sin cupo queda EN_ESPERA. También entra a la cola si queda un cupo libre pero hay personas esperando; la nueva llegada no las adelanta. La promoción automática corresponde a Fase 3 (#49).

La posición se guarda y nunca se recalcula al consultar o cancelar. Cancelar una EN_ESPERA deja un hueco y borra solo su posición. El GET conserva el orden ascendente de las posiciones persistidas, sin canceladas. Se mantienen el rechazo de duplicados activos (409) y la posibilidad de volver a reservar tras cancelar.

## Contradicción que debe aclararse en el issue

Las notas indican "número de EN_ESPERA + 1", pero también prohíben renumerar al cancelar. Con posiciones [1,2,3], cancelar la 1 deja [2,3]; el conteo + 1 asignaría de nuevo 3. Para cumplir estabilidad, FIFO y evitar empates, esta implementación usa MAX(posición EN_ESPERA) + 1 bajo el mismo lock de Clase. Si la cola está vacía asigna 1. Debe anotarse esta aclaración en #40 antes de aprobar el PR; no se presenta como cumplimiento literal de la fórmula incompatible.

El número indica orden persistido, no necesariamente cuántas personas hay delante después de una cancelación. No se cambia el contrato JSON.

Reserva y cancelación toman primero el lock de Clase dentro de la transacción, para que ninguna renumeración o llegada concurrente altere el orden. La cancelación consulta primero el id escalar de clase y carga la reserva después del lock, evitando modificar una entidad leída antes de adquirirlo.

Pruebas JPA: [1,2,3] -> cancelar 1 -> [2,3] -> llegada nueva -> [2,3,4]; consultas repetidas sin cambios; duplicados; no adelantar cola al liberarse cupo. Concurrencia: 8 hilos/1 cupo repetido 3 veces.
