-- C01. ¿Qué pedidos se han entregado, del más antiguo al más reciente?
select id_pedido, id_cliente, id_restaurante, fecha_pedido, total
from pedido
where estado = 'entregado'
order by fecha_pedido asc;

-- C02. ¿Qué pedidos fueron cancelados?
select id_pedido, id_cliente, id_restaurante, fecha_pedido, total, notas
from pedido
where estado = 'cancelado'
order by fecha_pedido asc;

-- C03. ¿Qué pedidos ha hecho Valentina Rojas (cliente 1), del más antiguo al más reciente?
select id_pedido, id_restaurante, estado, fecha_pedido, total
from pedido
where id_cliente = 1
order by fecha_pedido asc;

-- C04. ¿Qué productos ofrece Sazón Costeño (restaurante 1), del más barato al más caro?
select nombre, descripcion, precio, disponible
from producto
where id_restaurante = 1
order by precio asc;

-- C05. ¿Qué productos no están disponibles para la venta?
select id_restaurante, nombre, precio
from producto
where disponible = false
order by id_restaurante asc;

-- C06. ¿Qué restaurantes están abiertos y cuál es su calificación promedio?
select nombre, ciudad, calificacion_promedio
from restaurante
where estado = 'abierto'
order by calificacion_promedio asc;

-- C07. ¿Cuánto se ha vendido en total en pedidos entregados?
select sum(total)
from pedido
where estado = 'entregado';

-- C08. ¿Cuántas unidades se han vendido del arroz con coco y pescado frito (producto 1)?
select sum(cantidad)
from detalle_pedido
where id_producto = 1;

-- C09. ¿Qué calificaciones de 3 o menos estrellas se han recibido?
select id_pedido, id_restaurante, puntuacion, comentario, fecha
from calificacion
where puntuacion <= 3
order by puntuacion asc;

-- C10. ¿Qué domiciliarios están disponibles y en qué vehículo trabajan?
select id_domiciliario, vehiculo, fecha_registro
from domiciliario
where estado = 'disponible'
order by vehiculo asc;
