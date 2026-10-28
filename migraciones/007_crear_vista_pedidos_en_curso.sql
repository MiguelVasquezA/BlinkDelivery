create view pedidos_en_curso as
select pe.id_pedido,
       pe.fecha_pedido,
       pe.estado,
       cl.nombre as nombre_cliente,
       cl.apellido as apellido_cliente,
       r.nombre as restaurante,
       d.direccion_texto,
       d.ciudad,
       pe.id_domiciliario,
       pe.total
from pedido pe
join persona cl on cl.id_persona = pe.id_cliente
join restaurante r on r.id_restaurante = pe.id_restaurante
join direccion d on d.id_direccion = pe.id_direccion
where pe.estado <> 'entregado'
  and pe.estado <> 'cancelado';
