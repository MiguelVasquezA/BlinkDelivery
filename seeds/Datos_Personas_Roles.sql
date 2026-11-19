insert into persona (id_persona, nombre, apellido, telefono) values
(1, 'Valentina', 'Rojas', '3005550101'),
(2, 'Andrés', 'Peña', '3105550102'),
(3, 'Camila', 'Ortiz', null),
(4, 'Santiago', 'Herrera', '3015550104'),
(5, 'Mariana', 'Castillo', null),
(6, 'Julián', 'Mendoza', '3205550106'),
(7, 'Daniela', 'Fuentes', '3005550107'),
(8, 'Sebastián', 'Cárdenas', null),
(9, 'Laura', 'Benítez', '3115550109'),
(10, 'Felipe', 'Guerrero', null),
(11, 'Isabella', 'Navarro', '3045550111'),
(12, 'Tomás', 'Acosta', '3125550112'),
(13, 'Carlos', 'Pacheco', '3005550113'),
(14, 'Natalia', 'Salazar', '3145550114'),
(15, 'Luis', 'Barrios', '3155550115'),
(16, 'Paola', 'Jiménez', '3165550116'),
(17, 'Ricardo', 'Molina', '3175550117'),
(18, 'Gabriela', 'Torres', '3185550118');

insert into cliente (id_cliente, correo, contrasena_hash, fecha_registro) values
(1, 'valentina.rojas@ejemplo.com', 'hash_demo_01', '2026-03-05'),
(2, 'andres.pena@ejemplo.com', 'hash_demo_02', '2026-03-12'),
(3, 'camila.ortiz@ejemplo.com', 'hash_demo_03', '2026-04-01'),
(4, 'santiago.herrera@ejemplo.com', 'hash_demo_04', '2026-04-18'),
(5, 'mariana.castillo@ejemplo.com', 'hash_demo_05', '2026-05-02'),
(6, 'julian.mendoza@ejemplo.com', 'hash_demo_06', '2026-05-20'),
(7, 'daniela.fuentes@ejemplo.com', 'hash_demo_07', '2026-06-03'),
(8, 'sebastian.cardenas@ejemplo.com', 'hash_demo_08', '2026-06-15'),
(9, 'laura.benitez@ejemplo.com', 'hash_demo_09', '2026-07-01'),
(10, 'felipe.guerrero@ejemplo.com', 'hash_demo_10', '2026-07-20'),
(11, 'isabella.navarro@ejemplo.com', 'hash_demo_11', '2026-09-15'),
(12, 'tomas.acosta@ejemplo.com', 'hash_demo_12', '2026-08-01'),
(15, 'luis.barrios@ejemplo.com', 'hash_demo_15', '2026-08-25'),
(16, 'paola.jimenez@ejemplo.com', 'hash_demo_16', '2026-09-30');

insert into domiciliario (id_domiciliario, vehiculo, estado, fecha_registro) values
(13, 'moto', 'disponible', '2026-02-01'),
(14, 'bicicleta', 'en entrega', '2026-03-10'),
(15, 'moto', 'disponible', '2026-04-05'),
(16, 'carro', 'en entrega', '2026-05-12'),
(17, 'bicicleta', 'inactivo', '2026-03-20'),
(18, 'moto', 'disponible', '2026-06-01');