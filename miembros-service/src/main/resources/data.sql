-- =====================================================================
-- Datos de demo del servicio de Miembros y Membresías.
-- Se cargan al arrancar el servicio. ON CONFLICT DO NOTHING evita
-- duplicarlos si el servicio se reinicia sin limpiar la BD.
--
-- Para empezar limpio:
--   docker compose down -v && docker compose up -d
--   y arrancar los 3 servicios.
--
-- IDs fijos (clases-service referencia 1, 2, 3, 4 en su data.sql):
--   1 = Ana    BASICO  SUSPENDIDA  → demo: rechazo de Strategy (Básico sin activar)
--   2 = Luis   FULL    ACTIVA      → demo: happy path de reservas
--   3 = Sara   PREMIUM SUSPENDIDA  → demo: plan ilimitado pero sin activar
--   4 = Diego  BASICO  ACTIVA      → demo: primero en lista de espera (paso 4)
--   5 = Carlos FULL    VENCIDA     → demo: /acceso activa=false por VENCIDA
--   6 = María  PREMIUM ACTIVA      → demo: miembro premium activo (reserva ilimitada)
--   7 = Pedro  (sin membresía)     → demo: /acceso activa=false, plan=null
-- =====================================================================

-- Membresías (una por miembro con plan)
INSERT INTO membresia (id, plan, estado, fecha_inicio, fecha_vencimiento) VALUES
                                                                              (1, 'BASICO',  'SUSPENDIDA', CURRENT_DATE, CURRENT_DATE + 30),
                                                                              (2, 'FULL',    'ACTIVA',     CURRENT_DATE, CURRENT_DATE + 90),
                                                                              (3, 'PREMIUM', 'SUSPENDIDA', CURRENT_DATE, CURRENT_DATE + 365),
                                                                              (4, 'BASICO',  'ACTIVA',     CURRENT_DATE, CURRENT_DATE + 30),
                                                                              (5, 'FULL',    'VENCIDA',    CURRENT_DATE - 120, CURRENT_DATE - 30),
                                                                              (6, 'PREMIUM', 'ACTIVA',     CURRENT_DATE, CURRENT_DATE + 365)
    ON CONFLICT (id) DO NOTHING;

-- Miembros (Pedro no tiene membresía: membresia_id NULL)
INSERT INTO miembro (id, nombre, email, fecha_registro, membresia_id) VALUES
                                                                          (1, 'Ana',   'ana@test.com',   NOW(), 1),
                                                                          (2, 'Luis',  'luis@test.com',  NOW(), 2),
                                                                          (3, 'Sara',  'sara@test.com',  NOW(), 3),
                                                                          (4, 'Diego', 'diego@test.com', NOW(), 4),
                                                                          (5, 'Carlos','carlos@test.com',NOW(), 5),
                                                                          (6, 'María', 'maria@test.com', NOW(), 6),
                                                                          (7, 'Pedro', 'pedro@test.com', NOW(), NULL)
    ON CONFLICT (id) DO NOTHING;

-- Avanzar las secuencias para que los próximos INSERT generen IDs > 7
SELECT setval('membresia_id_seq', (SELECT MAX(id) FROM membresia));
SELECT setval('miembro_id_seq',  (SELECT MAX(id) FROM miembro));