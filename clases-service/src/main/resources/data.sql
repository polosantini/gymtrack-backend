-- Datos de demo de clases-service (issue #18).
-- Se cargan al arrancar el servicio y se REFRESCAN en cada arranque:
-- ON CONFLICT (id) DO UPDATE reescribe las filas de demo, así que las fechas
-- siempre quedan ancladas a la semana en curso sin borrar la base de datos.
--
-- miembroId (referencia lógica a miembros-service, data.sql de A):
--   1 = Ana (BASICO), 2 = Luis (FULL), 3 = Sara (PREMIUM)
--   4 = Diego (BASICO, ACTIVA), 6 = María (PREMIUM, ACTIVA)
--
-- Las reservas no llevan fechas literales. Se calculan sobre el PRÓXIMO MARTES
-- (hoy mismo si hoy es martes), porque Spinning y Yoga se dictan MARTES y JUEVES:
--   CURRENT_DATE + ((9 - ISODOW(CURRENT_DATE)) % 7)
-- El desplazamiento de cada fila es en días respecto a ese martes:
--   0  = martes de esta semana (reservas vigentes de la demo)
--   -7 = martes anterior (historial de asistencia, siempre en el pasado)

INSERT INTO clase (id, nombre, instructor, dias, hora, duracion_minutos, aforo, sala) VALUES
                                                                                          (1, 'Spinning',  'Carlos Ruiz',  'MARTES,JUEVES',           '18:00', 45, 3,  'Sala 1'),
                                                                                          (2, 'Yoga',      'Laura Gómez',  'MARTES,JUEVES',           '07:00', 60, 10, 'Sala 2'),
                                                                                          (3, 'Funcional', 'Andrés Pérez', 'LUNES,MIERCOLES,VIERNES', '19:00', 50, 15, 'Sala 1')
    ON CONFLICT (id) DO UPDATE SET
    nombre            = EXCLUDED.nombre,
                            instructor        = EXCLUDED.instructor,
                            dias              = EXCLUDED.dias,
                            hora              = EXCLUDED.hora,
                            duracion_minutos  = EXCLUDED.duracion_minutos,
                            aforo             = EXCLUDED.aforo,
                            sala              = EXCLUDED.sala;

INSERT INTO reserva (id, miembro_id, clase_id, fecha, estado, posicion_espera, asistencia)
SELECT d.id, d.miembro_id, d.clase_id, martes.fecha + d.desplazamiento,
       d.estado, d.posicion_espera, d.asistencia
FROM (SELECT CURRENT_DATE + ((9 - EXTRACT(ISODOW FROM CURRENT_DATE)::int) % 7) AS fecha) AS martes,
     (VALUES
        -- Spinning del martes: aforo 3 LLENO + 1 en espera
        (1, 1, 1,  0, 'CONFIRMADA', NULL::int, NULL::boolean),
        (2, 2, 1,  0, 'CONFIRMADA', NULL,      NULL),
        (3, 3, 1,  0, 'CONFIRMADA', NULL,      NULL),
        (4, 4, 1,  0, 'EN_ESPERA',  1,         NULL),
        -- Yoga del martes: María (PREMIUM), 1 de 10 ocupado.
        -- Luis conserva solo Spinning ese día: una reserva por día.
        (5, 6, 2,  0, 'CONFIRMADA', NULL,      NULL),
        -- Historial de asistencia (Spinning del martes anterior)
        (6, 1, 1, -7, 'CONFIRMADA', NULL,      TRUE),
        (7, 2, 1, -7, 'CONFIRMADA', NULL,      FALSE)
     ) AS d(id, miembro_id, clase_id, desplazamiento, estado, posicion_espera, asistencia)
ON CONFLICT (id) DO UPDATE SET
    miembro_id      = EXCLUDED.miembro_id,
                        clase_id        = EXCLUDED.clase_id,
                        fecha           = EXCLUDED.fecha,
                        estado          = EXCLUDED.estado,
                        posicion_espera = EXCLUDED.posicion_espera,
                        asistencia      = EXCLUDED.asistencia;

-- Las reservas creadas durante la demo (id 8 en adelante) no se tocan.
SELECT setval('clase_id_seq',   (SELECT MAX(id) FROM clase));
SELECT setval('reserva_id_seq', (SELECT MAX(id) FROM reserva));
