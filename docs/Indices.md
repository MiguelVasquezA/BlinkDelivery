# Documentación de Índices - BlinkDelivery

En el desarrollo de este proyecto y conforme a los temas vistos en clase sobre gestión de bases de datos relacionales, se ha optado por apoyarnos en los índices que PostgreSQL crea de forma automática por cada restricción de tipo **PRIMARY KEY** y **UNIQUE**.

Estos índices garantizan la unicidad de los datos y optimizan las búsquedas principales sin requerir instrucciones adicionales de indexación manual (`CREATE INDEX`).

## Índices automáticos en el esquema

A continuación se enlistan los índices generados por el motor de base de datos a partir de las migraciones (001 a 006):

| Tabla | Índice | Columna | Tipo / Origen |
| :--- | :--- | :--- | :--- |
| persona | persona_pkey | id_persona | PRIMARY KEY |
| cliente | cliente_pkey | id_cliente | PRIMARY KEY |
| cliente | correo_cliente_unico | correo | UNIQUE |
| domiciliario | domiciliario_pkey | id_domiciliario | PRIMARY KEY |
| direccion | direccion_pkey | id_direccion | PRIMARY KEY |
| restaurante | restaurante_pkey | id_restaurante | PRIMARY KEY |
| categoria | categoria_pkey | id_categoria | PRIMARY KEY |
| categoria | nombre_categoria_unico | nombre | UNIQUE |
| restaurante_categoria | restaurante_categoria_pkey | id_restaurante, id_categoria | PRIMARY KEY compuesta |
| producto | producto_pkey | id_producto | PRIMARY KEY |
| pedido | pedido_pkey | id_pedido | PRIMARY KEY |
| detalle_pedido | detalle_pedido_pkey | id_detalle_pedido | PRIMARY KEY |
| calificacion | calificacion_pkey | id_calificacion | PRIMARY KEY |
| calificacion | calificacion_por_pedido_unica | id_pedido | UNIQUE |

## Conclusión

El uso de llaves primarias y restricciones únicas asegura que el sistema mantenga la integridad referencial y un rendimiento eficiente en las consultas básicas de búsqueda por identificador o atributos únicos esenciales, cumpliendo estrictamente con los conceptos fundamentales evaluados en clase.