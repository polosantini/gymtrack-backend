# Strategy y OCP (GitHub #35)

Reglas en `domain/strategy/`: `ReglasReservaStrategy`, `ReglaBasico`, `ReglaFull`, `ReglaPremium` y `ReglasReservaResolver`.

Básico permite 2 reservas en la semana de la sesión (lunes a domingo, ambos incluidos). Full permite 1 por día de la sesión. Premium no impone límites de plan; siguen vigentes duplicados y aforo. El conteo es por miembro, entre todas las clases, y solo incluye CONFIRMADA y EN_ESPERA; excluye CANCELADA.

Las fechas del contrato son LocalDate de America/Bogota: no se convierten a UTC ni se usa la fecha actual del servidor para decidir la ventana. La semana puede cruzar meses y años.

El resolver registra la lista de estrategias inyectadas mediante un mapa; no contiene switch por plan. Para agregar un plan basta una nueva implementación registrada como bean. La prueba de extensibilidad agrega un cuarto plan sin editar el resolver (evidencia OCP).

Pruebas unitarias de las tres reglas, resolver y prueba JPA del conteo inclusivo. El flujo HTTP real requiere #36 y #37: no se añade un campo plan al request ni se fija un plan temporal en producción. No cerrar el criterio curl hasta integrar esos PRs y ejecutar la evidencia indicada en la guía final.

Validación de este paso: reactor Maven completo correcto, sin fallos. El entorno de ejecución usa Java 21 con source/target 21 (se desactiva --release porque su instalación no trae ct.sym) y agente de Mockito explícito; los POM del proyecto no cambian. En un JDK 21 completo se usa `mvn test`.
