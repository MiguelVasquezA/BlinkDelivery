insert into persona (id_persona, nombre, apellido, telefono)
select i, 'Usuario', 'Volumen', null
from generate_series(1001, 21000) as i;

insert into cliente (id_cliente, correo, contrasena_hash, fecha_registro)
select i, 'usuario' || i || '@ejemplo.com', 'hash_volumen', '2025-01-01 08:00'
from generate_series(1001, 21000) as i;

insert into direccion (id_direccion, id_cliente, alias, direccion_texto, ciudad, referencia, es_principal)
select i, i, 'Casa', 'Calle de prueba', 'Barranquilla', null, true
from generate_series(1001, 21000) as i;

insert into pedido (id_pedido, id_cliente, id_restaurante, id_domiciliario, id_direccion, estado, fecha_pedido, total, notas)
select i, 1001 + i % 20000, 1 + i % 5, 13, 1001 + i % 20000, 'entregado', '2026-06-01 12:00',
30000, null
from generate_series(1001, 201000) as i;

analyze;
