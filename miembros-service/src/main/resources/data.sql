INSERT INTO membresia (id, plan, estado, fecha_inicio, fecha_vencimiento) VALUES
                                                                              (1, 'BASICO',  'SUSPENDIDA', CURRENT_DATE, CURRENT_DATE + 30),
                                                                              (2, 'FULL',    'ACTIVA',     CURRENT_DATE, CURRENT_DATE + 90),
                                                                              (3, 'PREMIUM', 'SUSPENDIDA', CURRENT_DATE, CURRENT_DATE + 365)
    ON CONFLICT (id) DO NOTHING;

INSERT INTO miembro (id, nombre, email, fecha_registro, membresia_id) VALUES
                                                                          (1, 'Ana',  'ana@test.com',  NOW(), 1),
                                                                          (2, 'Luis', 'luis@test.com', NOW(), 2),
                                                                          (3, 'Sara', 'sara@test.com', NOW(), 3)
    ON CONFLICT (id) DO NOTHING;


SELECT setval('membresia_id_seq', (SELECT MAX(id) FROM membresia));
SELECT setval('miembro_id_seq',  (SELECT MAX(id) FROM miembro));