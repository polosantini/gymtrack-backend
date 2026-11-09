# Consulta de acceso (GitHub #37)

Clases consulta únicamente `GET {MIEMBROS_BASE_URL}/api/miembros/{id}/acceso`. Por defecto usa http://localhost:8081. No llama a Pagos ni replica membresías. Conexión: 2 s; lectura: 3 s.

`activa=false` produce 409 con "La membresía no está activa". Error de transporte, timeout, respuesta inválida o error del servidor de Miembros produce 503 con "No se pudo validar la membresía, intente de nuevo". Miembro inexistente produce 404. Una membresía activa sin plan reconocido falla de forma cerrada con 503.

El plan procede exclusivamente de Miembros y se resuelve en `ReservaServiceImpl`. La factoría inyecta la estrategia elegida en el constructor del caso de uso. Este aplica la regla antes del aforo dentro de la transacción del servicio. ReservaRequest no incorpora plan; cualquier campo adicional del frontend carece de autoridad.

La guía final incluye pasos de Postman para comprobar miembro activo, inactivo y Miembros apagado. Las pruebas automatizadas no sustituyen afirmar que alguien ejecutó Postman manualmente.
