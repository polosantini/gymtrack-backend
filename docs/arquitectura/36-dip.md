# DIP en la reserva de clases (GitHub #36)

`ReservarClaseUseCase` recibe `ReglasReservaStrategy` en su constructor y no importa reglas concretas. La factoría construye un caso por petición; el plan se resuelve fuera del caso de uso. Esto evita almacenar una estrategia mutable compartida entre peticiones.

La prueba `ReservarClaseUseCaseTest` inyecta una estrategia falsa y verifica que se ejecuta antes de consultar el aforo. Si rechaza, no se guarda una reserva. No es necesario modificar el caso de uso para añadir otro plan.

Este PR prepara el caso de uso. La conexión del endpoint al plan real de Miembros se entrega en #37, manteniendo la separación entre issues. #37 reemplaza la lógica anterior del servicio por la delegación a este caso de uso.
