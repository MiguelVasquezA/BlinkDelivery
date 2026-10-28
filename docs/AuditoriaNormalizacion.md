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


### restaurante

- Claves candidatas: `id_restaurante`.
- DF: `id_restaurante →` todas las demás columnas.
- DF que no se cumplen:
  - `nombre ↛ id_restaurante`: una cadena puede tener dos sedes con el mismo nombre.
  - `direccion_texto ↛ ciudad`: igual que en `direccion`.
- Verificación: dentro de la tabla, el único determinante es la clave. **BCNF.**
- Observación: `calificacion_promedio` es un dato derivado de `calificacion` (ver Datos derivados).

### categoria

- Claves candidatas: `id_categoria` y `nombre` (`nombre_categoria_unico`).
- DF: `id_categoria → nombre`; `nombre → id_categoria`.
- Verificación: **BCNF.**

### restaurante_categoria

- Claves candidatas: `(id_restaurante, id_categoria)`.
- DF: solo triviales.
- DF que no se cumplen:
  - `id_restaurante ↛ id_categoria`: La Brasa del Prado está en Asados y en Comida típica.
  - `id_categoria ↛ id_restaurante`: Comida rápida tiene a Burger Norte y a Pizzería Bella Napoli.
- 2FN: es la única tabla con clave compuesta, pero no tiene atributos no primos.
- Verificación: **BCNF.** Además existe por la 1FN: guardar las categorías como lista dentro de
  `restaurante` violaría la atomicidad.

### producto

- Claves candidatas: `id_producto`.
- DF: `id_producto → id_restaurante, nombre, descripcion, precio, disponible`.
- DF que no se cumplen: `nombre ↛ precio`: una "Gaseosa" puede costar distinto en cada restaurante.
- Observación: si se declarara que un restaurante no repite nombres de producto,
  `(id_restaurante, nombre)` sería otra clave candidata, y la tabla seguiría en BCNF.
- Verificación: **BCNF.**