# ISP en Clases y Reservas (GitHub #38)

`ClaseQueryService` expone solo listar, detalle y consulta de reservas por clase/fecha. `ClaseQueryServiceImpl` calcula los cupos desde el repositorio sin depender de ReservaService. `ClaseService` y su implementación se ocupan exclusivamente de crear clases.

`ReservaService` mantiene reserva, cancelación, asistencia e historial del miembro. Su implementación está separada de la consulta del catálogo. ClaseController usa el servicio de escritura y la interfaz de consultas; no importa ReservaService. ReservaController no conoce ClaseQueryService.

La ruta GET /api/clases/{id}/reservas?fecha= se mueve al controlador de clases conservando URL, JSON, errores y orden por posición. No requiere cambios del frontend. Las pruebas de esas consultas se trasladan al servicio/controlador correspondiente.

Evidencia ISP para el informe: un consumidor del catálogo no depende de operaciones de reserva o cancelación. Las interfaces pueden cambiar o probarse separadamente. Las consultas de próxima sesión usan explícitamente America/Bogota.
