-- C01. ¿Qué pedidos están en curso en este momento, del más antiguo al más reciente?
select *
from pedidos_en_curso
order by fecha_pedido;

-- C02. ¿Qué pedidos en curso todavía no tienen domiciliario asignado?
select id_pedido, estado, restaurante, direccion_texto, ciudad
from pedidos_en_curso
where id_domiciliario is null
order by fecha_pedido;

-- C03. ¿Qué productos ha comprado Valentina Rojas, en qué restaurante y cuánto pagó por cada línea?
select pe.id_pedido,
       pe.fecha_pedido,
       r.nombre as restaurante,
       pr.nombre as producto,
       dp.cantidad,
       dp.precio_unitario,
       dp.cantidad * dp.precio_unitario as subtotal
from pedido pe
join detalle_pedido dp on dp.id_pedido = pe.id_pedido
join producto pr on pr.id_producto = dp.id_producto
join restaurante r on r.id_restaurante = pe.id_restaurante
where pe.id_cliente = 1
order by pe.fecha_pedido, pr.nombre;

-- C04. ¿Cuántas entregas ha completado cada domiciliario y con qué vehículo trabaja?
select p.nombre,
       p.apellido,
       d.vehiculo,
       count(pe.id_pedido) as entregas
from domiciliario d
join persona p on p.id_persona = d.id_domiciliario
join pedido pe on pe.id_domiciliario = d.id_domiciliario
where pe.estado = 'entregado'
group by d.id_domiciliario, p.nombre, p.apellido, d.vehiculo
order by entregas desc, p.apellido;

-- C05. ¿Qué clientes registrados nunca han hecho un pedido?
select p.nombre, p.apellido, c.correo, c.fecha_registro
from cliente c
join persona p on p.id_persona = c.id_cliente
left join pedido pe on pe.id_cliente = c.id_cliente
where pe.id_pedido is null
order by c.fecha_registro;

-- C06. ¿Qué productos no se han vendido nunca, y de qué restaurante son?
select r.nombre as restaurante, pr.nombre as producto, pr.precio, pr.disponible
from producto pr
join restaurante r on r.id_restaurante = pr.id_restaurante
left join detalle_pedido dp on dp.id_producto = pr.id_producto
where dp.id_detalle_pedido is null
order by r.nombre, pr.nombre;

-- C07. ¿Qué restaurantes tienen más de 3 pedidos entregados, y cuánto han vendido?
select r.nombre,
       count(pe.id_pedido) as pedidos_entregados,
       sum(pe.total) as ventas
from restaurante r
join pedido pe on pe.id_restaurante = r.id_restaurante
where pe.estado = 'entregado'
group by r.id_restaurante, r.nombre
having count(pe.id_pedido) > 3
order by ventas desc;

-- C08. ¿Qué pedidos entregados superan el valor promedio de los pedidos entregados?
select id_pedido, fecha_pedido, total
from pedido
where estado = 'entregado'
  and total > (select avg(total) from pedido where estado = 'entregado')
order by total desc;

-- C09. ¿Cuánto ha gastado cada cliente en pedidos entregados, de mayor a menor?
select p.nombre,
       p.apellido,
       count(pe.id_pedido) as pedidos,
       sum(pe.total) as total_gastado
from pedido pe
join persona p on p.id_persona = pe.id_cliente
where pe.estado = 'entregado'
group by p.id_persona, p.nombre, p.apellido
order by total_gastado desc;

-- C10. ¿Qué calificaciones de 3 o menos estrellas se han recibido, de quién y para qué restaurante?
select r.nombre as restaurante,
       p.nombre,
       p.apellido,
       ca.puntuacion,
       ca.comentario,
       ca.fecha
from calificacion ca
join restaurante r on r.id_restaurante = ca.id_restaurante
join persona p on p.id_persona = ca.id_cliente
where ca.puntuacion <= 3
order by ca.puntuacion, ca.fecha;
