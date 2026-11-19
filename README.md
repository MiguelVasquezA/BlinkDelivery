# Blink Delivery: Sistema de Gestión de Domicilios

Proyecto Integrador de la asignatura **Base de Datos I**.

## Integrantes

- Miguel Angel Vasquez Aguirre (Grupo 05)
- Adriana Dangond Cantillo (Grupo 06)
- Ednilson Contreras (Grupo 05)
- Alejandro Enrique Ochoa Sivera (Grupo 05)
- Iván Camilo Hernández (Grupo 07)
- Abraham Medina Rodriguez (Grupo 06)

## Descripción

Blink Delivery es la base de datos de una plataforma de domicilios. Permite administrar
clientes, direcciones, restaurantes, categorías, productos, domiciliarios, pedidos y
calificaciones.

Hoy la información para pedir comida a domicilio está repartida entre distintos medios, y el
cliente no puede comparar con facilidad tipo de comida, precios, disponibilidad y
calificaciones, ni saber en qué estado va su pedido. La plataforma centraliza esa información
y la mantiene relacionada: cada pedido queda asociado al cliente que lo hace, al restaurante
que lo prepara, a los productos que incluye, a la dirección de entrega y al domiciliario que
lo lleva.

La base de datos debe poder responder preguntas como:

- ¿Cuáles son los restaurantes mejor calificados?
- ¿Qué productos ofrece cada restaurante, a qué precio y con qué disponibilidad?
- ¿Qué pedidos ha hecho un cliente y qué productos incluye cada uno?
- ¿Qué pedidos están en proceso de entrega?

## Alcance

- El enfoque principal es la experiencia del cliente: buscar, comparar y pedir.
- El seguimiento del pedido se hace por estados, no por geolocalización en tiempo real (SP06).
- Un pedido es de un solo restaurante y tiene como máximo un domiciliario (RN09, RN10).
- Las recomendaciones automáticas de restaurantes o productos son una funcionalidad opcional
  y no forman parte del núcleo del sistema (SP07).

## Tecnologías y herramientas utilizadas

- PostgreSQL
- Git y GitHub
- Mermaid (esquema relacional)
- SVG (diagrama EER)
- Microsoft Excel (diccionario de datos)

## Estructura del repositorio

```text
docs/
├── 1.PlanteamientoDelProblema.md
├── 2.RequisitosySupuestos.md
├── Diagrama-EER.svg
├── EsquemaRelacional.md
├── diccionario_datos_domicilios  (2).xlsx
├── CorreccionesEntrega1.md
├── AuditoriaNormalizacion.md
└── ReglasPendientes.md
migraciones/
├── 001_crear_personas_y_roles.sql
├── 002_crear_direcciones.sql
├── 003_crear_restaurantes_y_categorias.sql
├── 004_crear_productos.sql
├── 005_crear_pedidos.sql
├── 006_crear_calificaciones.sql
└── 008_crear_indices.sql
seeds/
├── Datos_Personas_Roles.sql
├── Datos_direcciones.sql
├── Datos_restaurantes_y_categorias.sql
├── Datos_productos.sql
├── Datos_pedidos.sql
├── Datos_calificaciones.sql
└── datos_volumen.sql
consultas/
└── banco_consultas.sql
```

## Documentación

| Archivo | Contenido |
|---|---|
| `1.PlanteamientoDelProblema.md` | Contexto, problemática, propuesta de solución y resultado esperado |
| `2.RequisitosySupuestos.md` | Requisitos, reglas de negocio, preguntas de análisis y supuestos |
| `Diagrama-EER.svg` | Diagrama entidad-relación extendido |
| `EsquemaRelacional.md` | Esquema relacional con claves, tipos y política de borrado |
| `diccionario_datos_domicilios  (2).xlsx` | Diccionario de datos de todas las tablas |
| `CorreccionesEntrega1.md` | Cambios hechos a partir de las observaciones de la Entrega 1 |
| `AuditoriaNormalizacion.md` | Dependencias funcionales y forma normal de cada tabla |
| `ReglasPendientes.md` | Reglas de negocio que se protegerán en la Entrega 3 |

## Requisitos

El detalle está en `docs/2.RequisitosySupuestos.md`:

- **Requisitos funcionales (RF01 a RF16):** gestión de personas, clientes, direcciones,
  restaurantes, categorías, productos, pedidos y su detalle, domiciliarios y su asignación,
  seguimiento e historial de pedidos, calificaciones y disponibilidad.
- **Requisitos no funcionales (RNF01 a RNF06):** integridad, persistencia, escalabilidad,
  disponibilidad, mantenibilidad y seguridad.
- **Reglas de negocio (RN01 a RN27):** cardinalidades entre entidades, estados permitidos,
  condiciones para calificar y reglas de la generalización de Persona.
- **Supuestos (SP01 a SP11).**

## Modelo de datos

### Entidades

| Tabla | Qué representa |
|---|---|
| `persona` | Datos personales (nombre, apellido y teléfono), registrados una sola vez |
| `cliente` | Rol de cliente de una persona: correo, contraseña y fecha de registro |
| `domiciliario` | Rol de domiciliario de una persona: vehículo, estado y fecha de registro |
| `direccion` | Direcciones de entrega de cada cliente |
| `restaurante` | Restaurantes de la plataforma, con su estado y calificación promedio |
| `categoria` | Tipos de comida |
| `restaurante_categoria` | Relación muchos a muchos entre restaurantes y categorías |
| `producto` | Productos del menú de cada restaurante, con precio y disponibilidad |
| `pedido` | Pedido de un cliente a un restaurante, con dirección, domiciliario, estado y total |
| `detalle_pedido` | Productos de cada pedido, con cantidad y precio unitario de la compra |
| `calificacion` | Puntuación de 1 a 5 que un cliente da a un restaurante por un pedido |

### Generalización de Persona

Cliente y Domiciliario son subtipos de Persona. La generalización es **total y solapada**
(RN25): toda persona es cliente, domiciliario o ambos, y una misma persona puede tener los dos
roles sin duplicar sus datos. `id_cliente` e `id_domiciliario` son a la vez clave primaria y
clave foránea hacia `persona.id_persona` (SP11).

### Estados

| Tabla | Valores permitidos |
|---|---|
| `pedido` | recibido, aceptado, en preparación, asignado, en camino, entregado, cancelado |
| `restaurante` | abierto, cerrado |
| `domiciliario` | disponible, en entrega, inactivo |
| `domiciliario.vehiculo` | moto, bicicleta, carro |

### Restricciones

Las migraciones protegen los datos con:

- Claves primarias y claves foráneas en todas las relaciones.
- `not null` en los datos obligatorios y `unique` donde no se admiten repetidos, como el correo
  del cliente o una sola calificación por pedido.
- `check` con nombre para estados, puntuaciones, cantidades y valores no negativos.
- `default` para valores iniciales, como el estado `recibido` del pedido.
- Política de borrado:
  - **Restringir** cuando el registro forma parte del historial (pedidos, entregas,
    calificaciones, ventas) o cuando quitar un rol debe ser una acción explícita.
  - **Cascada** solo para lo que no existe sin su padre: dirección, línea de pedido,
    calificación, producto y `restaurante_categoria`.

### Reglas pendientes

Algunas reglas dependen de otras filas o de otras tablas y no se pueden expresar con
restricciones declarativas. Están registradas en `docs/ReglasPendientes.md` y se protegerán en
la Entrega 3:

- RN09: un pedido solo tiene productos de su mismo restaurante.
- RN12: solo se califica un restaurante donde se compró.
- RN25: no hay personas sin rol.
- RN26: el domiciliario debe tener teléfono.
- RN27: el pedido va a una dirección del mismo cliente.
- Una sola dirección principal por cliente.
- El total del pedido es la suma de su detalle.
- El promedio del restaurante coincide con sus calificaciones.

## Normalización

`docs/AuditoriaNormalizacion.md` revisa cada tabla: claves candidatas, dependencias
funcionales y forma normal alcanzada.

- Todas las tablas están en BCNF, excepto `pedido`.
- `pedido` queda en 2FN por la dependencia `id_direccion → id_cliente`. Se mantiene como
  excepción justificada: el cliente es un hecho propio del pedido, la dirección nunca cambia
  de dueño y es la consulta más frecuente. El riesgo que queda se controla con la RN27.
- Los datos derivados (`pedido.total` y `restaurante.calificacion_promedio`) se guardan por
  rendimiento y se documenta cómo se controlarán.

## Base de datos

### Migraciones

La carpeta `migraciones/` contiene el esquema de la base de datos en scripts numerados.

| Archivo | Contenido |
|---|---|
| `001_crear_personas_y_roles.sql` | Tablas `persona`, `cliente` y `domiciliario` |
| `002_crear_direcciones.sql` | Tabla `direccion` |
| `003_crear_restaurantes_y_categorias.sql` | Tablas `restaurante`, `categoria` y `restaurante_categoria` |
| `004_crear_productos.sql` | Tabla `producto` |
| `005_crear_pedidos.sql` | Tablas `pedido` y `detalle_pedido` |
| `006_crear_calificaciones.sql` | Tabla `calificacion` |
| `008_crear_indices.sql` | Índice `idx_pedido_cliente` |

Los scripts deben ejecutarse en orden, porque algunas tablas dependen de otras mediante claves
foráneas.

### Datos semilla

La carpeta `seeds/` contiene los datos de prueba del sistema. Se cargan después de las
migraciones y en este orden:

1. `Datos_Personas_Roles.sql`: personas, clientes y domiciliarios.
2. `Datos_direcciones.sql`: direcciones de los clientes.
3. `Datos_restaurantes_y_categorias.sql`: restaurantes, categorías y su relación.
4. `Datos_productos.sql`: productos de cada restaurante.
5. `Datos_pedidos.sql`: pedidos y su detalle.
6. `Datos_calificaciones.sql`: calificaciones de los pedidos.

### Banco de consultas

`consultas/banco_consultas.sql` contiene las consultas de negocio, cada una con la pregunta
que responde:

| Consulta | Pregunta |
|---|---|
| C01 | ¿Qué pedidos se han entregado, del más antiguo al más reciente? |
| C02 | ¿Qué pedidos fueron cancelados? |
| C03 | ¿Qué pedidos ha hecho Valentina Rojas, del más antiguo al más reciente? |
| C04 | ¿Qué productos ofrece Sazón Costeño, del más barato al más caro? |
| C05 | ¿Qué productos no están disponibles para la venta? |
| C06 | ¿Qué restaurantes están abiertos y cuál es su calificación promedio? |
| C07 | ¿Cuánto se ha vendido en total en pedidos entregados? |
| C08 | ¿Cuántas unidades se han vendido del arroz con coco y pescado frito? |
| C09 | ¿Qué calificaciones de 3 o menos estrellas se han recibido? |
| C10 | ¿Qué domiciliarios están disponibles y en qué vehículo trabajan? |

Se ejecutan después de las migraciones (001 a 008) y de los datos semilla. Cada consulta se
ejecuta por separado.

### Índices

Los índices se crean con `migraciones/008_crear_indices.sql`.

`seeds/datos_volumen.sql` agrega 20 000 clientes y 200 000 pedidos de prueba para medir el
efecto de los índices. Solo se carga en una base aparte, creada para esa medición, después de
las migraciones y de los datos semilla; nunca en la base del banco de consultas.

## Cómo reconstruir la base de datos

1. Crear una base de datos vacía en PostgreSQL.
2. Ejecutar las migraciones de la `001` a la `008`, en orden.
3. Ejecutar los datos semilla de `seeds/`, en el orden indicado arriba.
4. Ejecutar las consultas de `consultas/banco_consultas.sql`.

Cada archivo se ejecuta una sola vez. Si se ejecuta dos veces, los datos semilla fallan por
claves repetidas y hay que recrear la base.

## Entregas

### Entrega 1

- Planteamiento del problema
- Requisitos, preguntas y supuestos
- Diagrama EER
- Esquema relacional
- Diccionario de datos inicial

### Entrega 2

- Correcciones de la Entrega 1
- Migraciones del esquema
- Datos semilla
- Auditoría de normalización
- Banco de consultas
- Índices y datos de volumen para medirlos
- Diccionario de datos actualizado

#### Correcciones de la Entrega 1

Cliente y Domiciliario repetían los mismos datos personales (nombre, apellido y teléfono). Si
una persona era cliente y también domiciliario, sus datos quedaban registrados dos veces y
podían contradecirse. Se incorporó el supertipo **Persona**, con Cliente y Domiciliario como
subtipos, en una generalización total y solapada (RN25).

Cambios por documento:

- **Diagrama EER:** se agregó Persona con `id_persona`, `nombre`, `apellido` y `teléfono`.
  Cliente y Domiciliario heredan esos datos. `teléfono` pasó de atributo multivaluado a
  atributo simple.
- **Esquema relacional:** se agregó la relación `persona`. `id_cliente` e `id_domiciliario`
  son a la vez clave primaria y clave foránea hacia `persona.id_persona`. Se documentó la
  política de borrado de todas las claves foráneas.
- **Diccionario de datos:** se agregaron las columnas de `persona`, se quitaron los datos
  personales de `cliente` y `domiciliario` y se agregó `vehiculo` a `domiciliario`.
- **Requisitos y supuestos:** se agregaron RF16, RN25, RN26 (el teléfono es obligatorio para el
  domiciliario) y SP11 (un único registro por persona).

El detalle está en `docs/CorreccionesEntrega1.md`.

### Entrega 3 (planeada)

Protección de las reglas pendientes mediante triggers y funciones.

## Control de versiones

- `main`: versión entregada del proyecto.
- `develop`: integración de las partes de cada entrega.
- Cada parte se trabaja en su propia rama (`feat/...`, `docs/...`) y entra a `develop` por
  pull request. Cuando todo está revisado en `develop`, se pasa a `main`.
- Los commits usan un prefijo de tipo: `feat`, `docs`, `fix`, `refactor`.

Etiqueta de la entrega:

```text
entrega-1
```

semilla de la carpeta `seeds/`. Cada consulta se ejecuta por separado.
