# Auditoría de normalización

## Método

Para cada tabla de las migraciones se siguieron los mismos pasos:

1. Escribir sus columnas tal como quedaron implementadas.
2. Encontrar sus claves candidatas: la clave primaria y cada columna o grupo declarado `unique`.
3. Listar las dependencias funcionales (DF) que impone el negocio, y también las que parecen
   cumplirse pero no, con un contraejemplo.
4. Verificar 1FN, 2FN, 3FN y BCNF: una tabla está en BCNF si, para toda DF no trivial X → A,
   X es superclave.
5. Concluir la forma normal alcanzada, o documentar la excepción con su justificación.

Las DF se obtienen de las reglas del negocio, no de los datos. Los datos semilla se usan para
lo contrario: un solo contraejemplo demuestra que una DF no existe.

Notación: `X → Y` significa que X determina a Y; `X ↛ Y`, que no lo determina.

## 1FN en todo el esquema

Todas las columnas son atómicas y no hay grupos repetidos: los productos de un pedido están en
`detalle_pedido` y las categorías de un restaurante en `restaurante_categoria`, no en columnas
`producto1`, `producto2`. El teléfono, que en el EER de la Entrega 1 era multivaluado, pasó a
ser un atributo simple en las correcciones.