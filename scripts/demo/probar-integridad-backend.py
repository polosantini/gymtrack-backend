"""Regresiones HTTP/AMQP del backend levantado con Compose; conserva fixtures de prueba."""
import base64
import concurrent.futures
import datetime as dt
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.request
import uuid
from zoneinfo import ZoneInfo

MIEMBROS = os.getenv('MIEMBROS_URL', 'http://localhost:8081').rstrip('/')
CLASES = os.getenv('CLASES_URL', 'http://localhost:8082').rstrip('/')
PAGOS = os.getenv('PAGOS_URL', 'http://localhost:8083').rstrip('/')
RABBIT = os.getenv('RABBIT_MANAGEMENT_URL', 'http://localhost:15672').rstrip('/')
SALIDA = Path(os.getenv('EVIDENCIA_INTEGRIDAD', 'docs/evidencias/integridad-backend/ejecucion.json'))
evidencia = {'resultado': 'PENDIENTE', 'pasos': [], 'fixtures': []}

def pedir(base, ruta, method='GET', body=None, rabbit=False):
    request = urllib.request.Request(base + ruta, method=method,
        data=None if body is None else json.dumps(body).encode())
    if body is not None:
        request.add_header('Content-Type', 'application/json')
    if rabbit:
        credentials = os.getenv('RABBIT_USER', 'gymtrack') + ':' + os.getenv('RABBIT_PASSWORD', 'gymtrack')
        request.add_header('Authorization', 'Basic ' + base64.b64encode(credentials.encode()).decode())
    # Los puertos publicados de Docker se consultan directamente, sin proxy HTTP del sistema.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(request, timeout=15) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)

def esperar(base, ruta, condicion):
    for _ in range(100):
        code, body = pedir(base, ruta)
        if code == 200 and condicion(body):
            return body
        time.sleep(.2)
    raise AssertionError('El estado no convergio: ' + ruta)

def exigir(condicion, mensaje):
    if not condicion:
        raise AssertionError(mensaje)

def publicar(routing, tipo, body, message_id):
    code, result = pedir(RABBIT, '/api/exchanges/%2F/gymtrack.events/publish', 'POST', {
        'properties': {'content_type': 'application/json', 'message_id': message_id,
                       'headers': {'__TypeId__': 'com.gymtrack.common.events.' + tipo}},
        'routing_key': routing, 'payload_encoding': 'string', 'payload': json.dumps(body)}, rabbit=True)
    exigir(code == 200 and result['routed'], 'Evento no enrutado: ' + routing)

def crear_miembro(plan):
    token = uuid.uuid4().hex
    code, m = pedir(MIEMBROS, '/api/miembros', 'POST', {'nombre': 'Integridad ' + plan,
        'email': 'integridad-' + token + '@fixture.test'})
    exigir(code == 201, 'Crear miembro: ' + str(m))
    code, _ = pedir(MIEMBROS, f"/api/miembros/{m['id']}/membresias", 'POST', {'plan': plan})
    exigir(code == 201, 'Crear membresia')
    code, pago = pedir(PAGOS, '/api/pagos', 'POST', {'miembroId': m['id'], 'plan': plan,
        'periodo': 'MENSUAL', 'monto': 50000 if plan == 'BASICO' else 80000, 'metodo': 'EFECTIVO'})
    exigir(code == 201 and pago['estado'] == 'REGISTRADO', 'Registrar pago')
    esperar(MIEMBROS, f"/api/miembros/{m['id']}/acceso", lambda r: r['activa'] and r['plan'] == plan)
    evidencia['fixtures'].append({'miembroId': m['id'], 'pagoId': pago['id'], 'plan': plan})
    return m['id'], pago

def probar_cuota(plan, limite):
    miembro, primer_pago = crear_miembro(plan)
    fecha = (dt.datetime.now(ZoneInfo('America/Bogota')).date() + dt.timedelta(days=1)).isoformat()
    ids = []
    for i in range(6):
        code, clase = pedir(CLASES, '/api/clases', 'POST', {'nombre': 'Integridad ' + plan + ' ' + str(i),
            'instructor': 'Prueba', 'dias': ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'],
            'hora': '18:00', 'duracionMinutos': 45, 'aforo': 10, 'sala': 'Integridad'})
        exigir(code == 201, 'Crear clase')
        ids.append(clase['id'])
    import threading
    inicio = threading.Barrier(len(ids))
    def reservar(clase_id):
        inicio.wait(timeout=15)
        return pedir(CLASES, '/api/reservas', 'POST', {'miembroId': miembro, 'claseId': clase_id, 'fecha': fecha})
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(ids)) as pool:
        results = list(pool.map(reservar, ids))
    exigir(sum(code == 201 for code, _ in results) == limite, 'Cuota incorrecta: ' + plan)
    exigir(all(code in (201, 409) for code, _ in results), 'Error inesperado en carrera: ' + str(results))
    code, historial = pedir(CLASES, f'/api/reservas?miembroId={miembro}')
    exigir(code == 200 and len(historial) == limite, 'Persistencia de cuota')
    evidencia['pasos'].append({'paso': 'cuota concurrente ' + plan, 'confirmadas': limite,
        'rechazadas': len(ids) - limite, 'miembroId': miembro})
    return miembro, primer_pago

def main():
    # La proteccion pertenece al servidor, incluso sin pasar por el formulario.
    token = uuid.uuid4().hex
    code, nuevo = pedir(MIEMBROS, '/api/miembros', 'POST', {'nombre': 'Integridad sin membresia',
        'email': 'sin-membresia-' + token + '@fixture.test'})
    exigir(code == 201, 'Crear miembro sin membresia')
    code, error = pedir(PAGOS, '/api/pagos', 'POST', {'miembroId': nuevo['id'], 'plan': 'FULL',
        'periodo': 'MENSUAL', 'monto': 80000, 'metodo': 'EFECTIVO', 'tieneMembresia': True})
    exigir(code == 409 and error.get('message') == 'El miembro no tiene una membresía creada',
        'El backend debe rechazar con 409 el pago sin membresia')
    code, historial = pedir(PAGOS, f"/api/pagos?miembroId={nuevo['id']}")
    exigir(code == 200 and historial == [], 'No debe quedar un pago rechazado guardado')
    evidencia['pasos'].append({'paso': 'pago sin membresia', 'status': 409, 'pagosGuardados': 0})
    miembro, a = probar_cuota('FULL', 1)
    probar_cuota('BASICO', 2)
    antes = pedir(MIEMBROS, f'/api/miembros/{miembro}')[1]['membresia']['fechaVencimiento']
    code, b = pedir(PAGOS, '/api/pagos', 'POST', {'miembroId': miembro, 'plan': 'FULL',
        'periodo': 'MENSUAL', 'monto': 80000, 'metodo': 'EFECTIVO'})
    exigir(code == 201, 'Segundo pago')
    renovado = esperar(MIEMBROS, f'/api/miembros/{miembro}', lambda r: r['membresia']['fechaVencimiento'] != antes)
    fecha = renovado['membresia']['fechaVencimiento']
    payload = {key: a[key] for key in ('miembroId', 'plan', 'periodo', 'monto', 'estado')}
    payload['pagoId'] = a['id']
    publicar('pago.registrado', 'PagoRegistrado', payload, 'pago.registrado:' + str(a['id']))
    # Enviar despues un tercer pago diferente actua como barrera de procesamiento.
    # El listener es serial por defecto; el evento A previo no debe sumar un periodo adicional.
    code, c = pedir(PAGOS, '/api/pagos', 'POST', {'miembroId': miembro, 'plan': 'PREMIUM',
        'periodo': 'MENSUAL', 'monto': 120000, 'metodo': 'EFECTIVO'})
    exigir(code == 201, 'Pago de barrera')
    final = esperar(MIEMBROS, f'/api/miembros/{miembro}', lambda r: r['membresia']['plan'] == 'PREMIUM')
    import calendar
    value = dt.date.fromisoformat(fecha)
    month = value.month % 12 + 1
    year = value.year + (value.month == 12)
    esperado = value.replace(year=year, month=month, day=min(value.day, calendar.monthrange(year, month)[1])).isoformat()
    exigir(final['membresia']['fechaVencimiento'] == esperado, 'Pago duplicado sumo otro periodo')
    code, _ = pedir(PAGOS, '/api/pagos?miembroId=abc')
    exigir(code == 400, 'Parametro invalido debe ser 400')
    evidencia['pasos'].append({'paso': 'duplicado A-B-A-C', 'vencimientoFinal': esperado, 'pagoB': b['id'], 'pagoC': c['id']})
    evidencia['pasos'].append({'paso': 'entrada invalida', 'status': code})
    evidencia['resultado'] = 'OK'

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        evidencia['resultado'] = 'FALLO'
        evidencia['error'] = str(error)
        raise
    finally:
        SALIDA.parent.mkdir(parents=True, exist_ok=True)
        SALIDA.write_text(json.dumps(evidencia, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(evidencia, ensure_ascii=False, indent=2))
