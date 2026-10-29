insert into restaurante (id_restaurante, nombre, descripcion, telefono, direccion_texto, ciudad,
estado, calificacion_promedio, fecha_registro) values
(1, 'Sazón Costeño', 'Comida típica de la costa: pescado, sancocho y fritos', '6053850101',
'Carrera 53 # 80-15', 'Barranquilla', 'abierto', 4.75, '2026-01-10 10:00'),
(2, 'La Brasa del Prado', 'Carnes a la parrilla y picadas', '6053850102', 'Calle 72 # 57-40',
'Barranquilla', 'abierto', 4.33, '2026-02-03 10:00'),
(3, 'Burger Norte', 'Hamburguesas, perros calientes y comida rápida', '6053850103', 'Calle 98 # 51-
20', 'Barranquilla', 'abierto', 3.67, '2026-02-20 10:00'),
(4, 'Dulce Tentación', 'Postres, tortas y café', '6053850104', 'Carrera 43 # 75-12',
'Barranquilla', 'abierto', 4.50, '2026-03-15 10:00'),
(5, 'Pizzería Bella Napoli', 'Pizzas artesanales y pastas', '6053850105', 'Calle 30 # 25-60',
'Soledad', 'cerrado', 2.50, '2026-04-02 10:00'),
(6, 'Wok Express', 'Comida oriental para llevar', null, 'Carrera 51B # 87-30', 'Barranquilla',
'abierto', null, '2026-10-01 10:00');

insert into categoria (id_categoria, nombre) values
(1, 'Comida típica'),
(2, 'Asados'),
(3, 'Hamburguesas'),
(4, 'Comida rápida'),
(5, 'Postres'),
(6, 'Pizzas'),
(7, 'Comida oriental'),
(8, 'Vegetariana');

insert into restaurante_categoria (id_restaurante, id_categoria) values
(1, 1),
(2, 2),
(2, 1),
(3, 3),
(3, 4),
(4, 5),
(5, 6),
(5, 4),
(6, 7);