-- C01. ¿Qué pedidos están en curso en este momento, del más antiguo al más reciente?
select *
from pedidos_en_curso
order by fecha_pedido;

-- C02. ¿Qué pedidos en curso todavía no tienen domiciliario asignado?
select id_pedido, estado, restaurante, direccion_texto, ciudad
from pedidos_en_curso
where id_domiciliario is null
order by fecha_pedido;
