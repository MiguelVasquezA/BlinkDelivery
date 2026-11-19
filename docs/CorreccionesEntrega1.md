# Correcciones de la Entrega 1

## Observación

Cliente y Domiciliario repetían los mismos datos personales (nombre, apellido y teléfono). Si una persona era cliente y también domiciliario, sus datos quedaban registrados dos veces y podían contradecirse.

## Decisión

Se incorporó el supertipo **Persona**, con Cliente y Domiciliario como subtipos. La generalización es **total y solapada** (RN25):

- **Total:** toda persona registrada es cliente, domiciliario o ambos.
- **Solapada:** una misma persona puede solicitar pedidos y también realizar entregas, sin una segunda cuenta ni datos duplicados.

## Qué cambió y por qué

### Diagrama EER (`Diagrama-EER.svg`)

- Se agregó **Persona** con `id_persona` (clave), `nombre`, `apellido` y `teléfono`.
- La generalización se representa con un círculo con **o** (solapada), una **línea doble** desde Persona (total) y el símbolo **∪** hacia cada subtipo.
- Cliente y Domiciliario dejaron de tener clave y datos personales propios, porque los heredan de Persona.
- Cliente conserva `correo`, `contraseña` y `fecha_registro`. Domiciliario conserva `vehículo`, `estado` y `fecha_registro`. Cada `fecha_registro` indica cuándo la persona empezó ese rol.
- `teléfono` pasó de atributo multivaluado a atributo simple, para coincidir con el esquema relacional, donde siempre fue una sola columna.
- Se agregó la relación **es destino de** (Dirección 1 : N Pedido), que faltaba aunque `pedido.id_direccion` ya existía en el esquema relacional.
- Los atributos se alinearon con el esquema relacional:
  - Restaurante: se quitó `nit`, que no existe en la tabla; `abierto` pasó a `estado` y `calificación` a `calificacion_promedio` (derivado). Se agregaron `descripción`, `teléfono`, `direccion_texto`, `ciudad` y `fecha_registro`.
  - Dirección: `calle` pasó a `direccion_texto` y se agregaron `alias`, `referencia` y `es_principal`.
  - Pedido: `fecha` pasó a `fecha_pedido` y se agregó `notas`.
  - Producto: se agregó `descripción`. Calificación: se agregó `fecha`.
- La participación total se marca con **línea doble**: las entidades débiles en su relación identificadora, el pedido en "contiene" (todo pedido tiene al menos un detalle) y cada entidad cuya clave foránea es obligatoria.
- Las cardinalidades usan solo `1`, `N` y `M`. "entrega" pasó a Domiciliario 1 : N Pedido, con participación parcial del pedido porque el domiciliario se asigna después; "genera" pasó a Pedido 1 : 1 Calificación, con participación parcial del pedido.
- Dirección y Detalle_Pedido siguen siendo entidades débiles, con claves parciales `consecutivo` y `línea`. En el esquema relacional se identifican con una clave sustituta (`id_direccion`, `id_detalle_pedido`) en lugar de la clave compuesta.
- Se reubicaron los atributos de Pedido, Producto y Categoría, que quedaban sobre las líneas de otras relaciones.

### Esquema relacional (`EsquemaRelacional.md`)

- Se agregó la relación `persona`.
- `id_cliente` e `id_domiciliario` siguen siendo las claves primarias de sus relaciones y ahora también son claves foráneas hacia `persona.id_persona`. Toman el valor de la persona y no se generan de forma independiente: así una persona tiene como máximo un registro por rol.
- Se documentó la política de borrado de todas las claves foráneas:
  - **Restringir** cuando el registro forma parte del historial (pedidos, entregas, calificaciones, ventas) o cuando quitar un rol debe ser una acción explícita.
  - **Cascada** solo para las entidades débiles o las asociaciones que no existen sin su padre (dirección, línea de pedido, calificación, producto, restaurante_categoria).

### Diccionario de datos (`diccionario_datos_domicilios .xlsx`)

- Se agregaron las columnas de `persona`, con su tipo, nulabilidad y significado.
- Se eliminaron `nombre`, `apellido` y `telefono` de `cliente` y `domiciliario`.
- Sus identificadores pasaron a `INTEGER` con llave `PK, FK` hacia `persona.id_persona`.
- `telefono` admite nulos porque es opcional para el cliente. Para el domiciliario es obligatorio (RN26).
- Se agregó `vehiculo` a `domiciliario` (`moto`, `bicicleta` o `carro`). Ya estaba en el diagrama EER, pero faltaba en el esquema relacional y en el diccionario.

### Requisitos y supuestos (`2.RequisitosySupuestos.md`)

- **RF16** (Gestión de Personas) y notas en RF01 y RF10.
- **RN25**: la generalización total y solapada.
- **RN26**: el teléfono es obligatorio para el domiciliario. Antes era una columna `NOT NULL` de `domiciliario`. Al trasladarse a `persona` debe admitir nulos, así que la obligación se conserva como regla del rol.
- **SP11**: un único registro por persona, cuyo identificador comparten sus roles.
- Preguntas de análisis sobre los roles de una persona, las personas sin rol, la ubicación de cada atributo y la obligatoriedad del teléfono.
