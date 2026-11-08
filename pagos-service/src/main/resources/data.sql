INSERT INTO pago (id, miembro_id, plan, periodo, monto, metodo, estado, fecha) VALUES
                                                                                   (1, 1, 'BASICO',  'MENSUAL',     50000.00,   'EFECTIVO',      'REGISTRADO', NOW()),
                                                                                   (2, 2, 'FULL',    'TRIMESTRAL',  216000.00,  'TARJETA',       'REGISTRADO', NOW()),
                                                                                   (3, 3, 'PREMIUM', 'ANUAL',       1152000.00, 'TRANSFERENCIA', 'REGISTRADO', NOW())
    ON CONFLICT (id) DO NOTHING;

INSERT INTO notificacion (id, miembro_id, tipo, mensaje, fecha, leida) VALUES
                                                                           (1, 1, 'CONFIRMACION_PAGO',  'Pago registrado correctamente',    NOW(), false),
                                                                           (2, 2, 'VENCIMIENTO_PROXIMO', 'Tu membresía vence en 7 días',    NOW(), false),
                                                                           (3, 3, 'CUPO_LIBERADO',       'Se liberó un cupo en tu clase',    NOW(), true)
    ON CONFLICT (id) DO NOTHING;

SELECT setval('pago_id_seq',         (SELECT MAX(id) FROM pago));
SELECT setval('notificacion_id_seq', (SELECT MAX(id) FROM notificacion));