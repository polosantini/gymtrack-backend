-- Datos de demo de clases-service (issue #18).
-- Se cargan al arrancar el servicio. ON CONFLICT DO NOTHING evita duplicarlos si ya existen.
-- La corrección de la reserva 5 también se aplica a datos de demo ya cargados, sin borrar la BD.
--
-- miembroId (referencia lógica a miembros-service, data.sql de A):
--   1 = Ana (BASICO), 2 = Luis (FULL), 3 = Sara (PREMIUM)
--   4 = Diego (BASICO, ACTIVA), 6 = María (PREMIUM, ACTIVA)
--
-- 2026-11-17 es martes (semana de la demo).

INSERT INTO clase (id, nombre, instructor, dias, hora, duracion_minutos, aforo, sala) VALUES
    (1, 'Spinning',  'Carlos Ruiz',  'MARTES,JUEVES',          '18:00', 45, 3,  'Sala 1'),
    (2, 'Yoga',      'Laura Gómez',  'MARTES,JUEVES',          '07:00', 60, 10, 'Sala 2'),
    (3, 'Funcional', 'Andrés Pérez', 'LUNES,MIERCOLES,VIERNES', '19:00', 50, 15, 'Sala 1')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO reserva (id, miembro_id, clase_id, fecha, estado, posicion_espera, asistencia) VALUES
    -- Spinning el martes 17: aforo 3 LLENO + 1 en espera
    (1, 1, 1, '2026-11-17', 'CONFIRMADA', NULL, NULL),
    (2, 2, 1, '2026-11-17', 'CONFIRMADA', NULL, NULL),
    (3, 3, 1, '2026-11-17', 'CONFIRMADA', NULL, NULL),
    (4, 4, 1, '2026-11-17', 'EN_ESPERA',  1,    NULL),
    -- Yoga el martes 17: María (PREMIUM), 1 de 10 ocupado; Luis conserva solo Spinning ese día
    (5, 6, 2, '2026-11-17', 'CONFIRMADA', NULL, NULL),
    -- Historial de asistencia (Spinning del martes 10)
    (6, 1, 1, '2026-11-10', 'CONFIRMADA', NULL, TRUE),
    (7, 2, 1, '2026-11-10', 'CONFIRMADA', NULL, FALSE)
    ON CONFLICT (id) DO NOTHING;

-- Corregir exclusivamente la reserva original de Yoga de la demo si ya estaba cargada.
-- ON CONFLICT DO NOTHING no modifica filas existentes. La condición evita tocar otros registros.
UPDATE reserva
SET miembro_id = 6
WHERE id = 5
  AND miembro_id = 2
  AND clase_id = 2
  AND fecha = '2026-11-17'
  AND estado = 'CONFIRMADA'
  AND posicion_espera IS NULL
  AND asistencia IS NULL;

SELECT setval('clase_id_seq',   (SELECT MAX(id) FROM clase));
SELECT setval('reserva_id_seq', (SELECT MAX(id) FROM reserva));
