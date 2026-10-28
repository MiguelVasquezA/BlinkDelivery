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


## Análisis por tabla

### persona

- Claves candidatas: `id_persona`.
- DF: `id_persona → nombre, apellido, telefono`.
- DF que no se cumplen:
  - `nombre, apellido ↛ telefono`: puede haber homónimos.
  - `telefono ↛ id_persona`: el teléfono no es único; dos personas podrían compartirlo.
- Verificación: el único determinante es la clave. **BCNF.**
- Antecedente: esta tabla nació de la corrección de la Entrega 1. Antes, nombre, apellido y
  teléfono estaban repetidos en cliente y domiciliario; la generalización eliminó esa redundancia.

### cliente

- Claves candidatas: `id_cliente` y `correo` (`correo_cliente_unico`).
- DF: `id_cliente → correo, contrasena_hash, fecha_registro`;
  `correo → id_cliente, contrasena_hash, fecha_registro`.
- Verificación: los dos determinantes son claves candidatas. **BCNF.** Tener dos claves
  candidatas no rompe ninguna forma normal.

### domiciliario

- Claves candidatas: `id_domiciliario`.
- DF: `id_domiciliario → vehiculo, estado, fecha_registro`.
- DF que no se cumplen: `vehiculo ↛ estado`. En los datos, los dos domiciliarios en bicicleta
  tienen estados distintos (`en entrega` e `inactivo`).
- Verificación: **BCNF.**

### direccion

- Claves candidatas: `id_direccion`.
- DF: `id_direccion → id_cliente, alias, direccion_texto, ciudad, referencia, es_principal`.
- DF que no se cumplen:
  - `direccion_texto ↛ ciudad`: cada municipio tiene su propia nomenclatura; "Calle 30 # 19-40"
    puede existir en Barranquilla y en Soledad.
  - `direccion_texto, ciudad ↛ id_cliente`: dos clientes pueden vivir en la misma dirección.
  - `id_cliente, alias ↛ id_direccion`: el alias es opcional y no es único.
- 1FN: `direccion_texto` se trata como valor atómico, porque el sistema nunca consulta por calle
  o número por separado.
- Verificación: **BCNF.**