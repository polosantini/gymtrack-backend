# Compatibilidad con el frontend de C

Referencia revisada sin modificar: gymtrack-frontend-main-Correcciones.zip, revision da1b765f6d138794cc71da6948d4a27b476095c7.

- RegistrarMiembroModal ahora crea primero POST /api/miembros con nombre/email y luego POST /api/miembros/{id}/membresias con plan. Compatible con Miembros.
- POST /api/pagos conserva miembroId, plan, periodo, monto y metodo. Retorna el mismo Pago con estado REGISTRADO en el caso valido.
- Los planes siguen siendo BASICO, FULL y PREMIUM; los periodos MENSUAL, TRIMESTRAL y ANUAL.
- El cliente pedir lee error.message y presenta 409/404/503. El rechazo nuevo no requiere una variable ni un flag adicional del frontend.
- El refresco de PagosPage espera 2500 ms despues del POST y ofrece Actualizar. Ese tiempo no es el timeout del servidor: la activacion sigue siendo asincrona y puede necesitar otra consulta si hay retrasos de cola.
- La consulta privada AMQP no expone nuevos campos al navegador.
- El modo mock del frontend aun debe alinearse por C con el nuevo rechazo de pagos sin membresia. Las pruebas de integracion deben ejecutarse con VITE_USAR_MOCK=false.
- useCarga y las otras mejoras de UI permanecen bajo responsabilidad de C; este ZIP solo cambia backend.
