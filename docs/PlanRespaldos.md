# Plan de respaldos – Blink Delivery

Proyecto Integrador de Base de Datos I · Entrega 3

Responsable de esta parte: Ednilson Contreras

## 1. Objetivo

Definir cómo se protege la información de la base de datos `blinkdelivery` ante pérdida o daño (borrado accidental, error en una migración o trigger, falla del equipo), y cómo se comprueba que esa protección funciona, restaurando los respaldos en una base aparte.

## 2. Qué se respalda

| Elemento | Detalle |
|---|---|
| Base de datos | `blinkdelivery` en PostgreSQL |
| Contenido | Esquema (11 tablas, restricciones, índices, y desde la Entrega 3, funciones y triggers) y todos sus datos |
| Tablas | `persona`, `cliente`, `domiciliario`, `direccion`, `restaurante`, `categoria`, `restaurante_categoria`, `producto`, `pedido`, `detalle_pedido`, `calificacion` |
| Fuera del respaldo | Código del repositorio (ya está versionado en Git) y archivos `.txt` de la aplicación Java |

El esquema también se puede reconstruir desde `migraciones/` y `seeds/`. El respaldo existe porque esos scripts solo recrean los datos de prueba, no los cambios hechos después sobre la base en uso.

## 3. Tipo de respaldo y herramienta

- **Tipo:** respaldo lógico completo con `pg_dump`.
- **Herramienta:** pgAdmin (opción *Backup...* sobre la base).
- **Formato:** *Custom* (archivo `.backup`), comprimido y compatible con la opción *Restore...* de pgAdmin y con `pg_restore`.
- **Nombre del archivo:** `blinkdelivery_AAAA-MM-DD.backup` (por ejemplo `blinkdelivery_2026-11-23.backup`).

Se eligió el respaldo lógico porque la base es pequeña, es portable entre equipos y versiones, y permite restaurar en una base distinta sin tocar la original.

## 4. Frecuencia

| Momento | Respaldo |
|---|---|
| Diario | Un respaldo completo al final de la jornada de trabajo |
| Antes de cambios grandes | Un respaldo adicional antes de ejecutar migraciones nuevas, crear triggers o funciones, o cargar datos masivos |
| Antes de la sustentación | Un respaldo final y su restauración probada, con la etiqueta `entrega-final` ya creada |

## 5. Retención

- Se conservan los **últimos 7 respaldos diarios**.
- Se conserva **1 respaldo semanal** durante 4 semanas.
- Los respaldos más antiguos se eliminan para no acumular archivos con datos personales sin necesidad.

## 6. Dónde se guardan

Se busca tener al menos dos copias en lugares distintos:

1. **Copia local:** carpeta de respaldos en el equipo del responsable, ubicada dentro de su carpeta de Documentos y fuera de la carpeta del repositorio. Esta carpeta se sincroniza con OneDrive, por lo que también queda una copia en la nube de la cuenta personal.
2. **Copia externa:** [memoria USB / equipo de otro integrante / One drive].

Los respaldos **no se suben a GitHub** (ver sección 8).

## 7. Metas de recuperación

| Meta | Valor | Significado |
|---|---|---|
| RPO (punto de recuperación) | 24 horas | Máximo de datos que se aceptaría perder: lo hecho desde el último respaldo diario |
| RTO (tiempo de recuperación) | 30 minutos | Tiempo máximo para dejar la base funcionando de nuevo a partir de un respaldo |

## 8. Seguridad de los respaldos

El archivo de respaldo contiene los mismos datos que la base, incluyendo datos personales (nombres, teléfonos, correos) y los hashes de contraseña de `cliente.contrasena_hash`. Por eso:

- No se versionan en el repositorio. El `.gitignore` excluye `*.backup`, `*.dump` y la carpeta `respaldos/`.
- Solo los integrantes del equipo tienen acceso a la carpeta de respaldos y a la copia externa.
- Las contraseñas de conexión a PostgreSQL no se escriben en este documento ni en el código.
- Los datos del repositorio son de prueba (`seeds/`), no datos reales de personas.

## 9. Ejecución del respaldo

Se realizó un respaldo completo de la base `blinkdelivery` desde pgAdmin, con los datos semilla ya cargados.

| Dato | Valor |
|---|---|
| Fecha y hora | [23/11/2026, hora en que lo hiciste] |
| Responsable | Ednilson Contreras |
| Base respaldada | `blinkdelivery` |
| Archivo generado | `blinkdelivery_2026-11-23.backup` |
| Formato | Custom |
| Tamaño del archivo | [tamaño, míralo en las propiedades del archivo] |
| Resultado | Exitoso |

### Procedimiento

1. Clic derecho sobre la base `blinkdelivery` y elegir **Backup...**
2. En **Filename**, se indicó el nombre del archivo y en **Format**, **Custom**.
3. Se presionó **Backup** y se verificó en **Processes** que terminó con éxito.

### Comando ejecutado por pgAdmin

```text
Backing up an object on the server 'PostgreSQL 18 (localhost: 5432)' from database 'blinkdelivery'
Running command:
C:\Program Files\PostgreSQL\18\pgAdmin 4\runtime\pg_dump.exe --file "C:\\Users\\EDNILS~1\\OneDrive\\DOCUME~1\\BLINKD~2" --host "localhost" --port "5432" --username "postgres" --no-password --format=c --large-objects --verbose
```

### Evidencias

**Evidencia 1:** configuración del respaldo.

![Configuración del respaldo](evidencias/01_backup_configuracion.png)

**Evidencia 2:** respaldo terminado con éxito.

![Respaldo exitoso](evidencias/02_backup_proceso_exitoso.png)

## 10. Estado de la base original antes de restaurar

Antes de restaurar, se registró el estado de la base `blinkdelivery` para poder compararlo con la base restaurada. Las consultas se ejecutaron sin hacer cambios entre el respaldo y la medición.

Consulta de conteos:

```sql
SELECT 'persona' AS tabla, COUNT(*) FROM persona
UNION ALL SELECT 'cliente', COUNT(*) FROM cliente
UNION ALL SELECT 'domiciliario', COUNT(*) FROM domiciliario
UNION ALL SELECT 'direccion', COUNT(*) FROM direccion
UNION ALL SELECT 'restaurante', COUNT(*) FROM restaurante
UNION ALL SELECT 'categoria', COUNT(*) FROM categoria
UNION ALL SELECT 'restaurante_categoria', COUNT(*) FROM restaurante_categoria
UNION ALL SELECT 'producto', COUNT(*) FROM producto
UNION ALL SELECT 'pedido', COUNT(*) FROM pedido
UNION ALL SELECT 'detalle_pedido', COUNT(*) FROM detalle_pedido
UNION ALL SELECT 'calificacion', COUNT(*) FROM calificacion;
```

Consulta de negocio (C07 del banco de consultas, total vendido en pedidos entregados):

```sql
SELECT SUM(total) FROM pedido WHERE estado = 'entregado';
```

Resultados en la base original:

| Tabla | Filas en `blinkdelivery` | Filas en `blinkdelivery_restore` | ¿Coincide? |
|---|---|---|---|
| persona | [18] | | |
| cliente | [14] | | |
| domiciliario | [6] | | |
| direccion | [17] | | |
| restaurante | [6] | | |
| categoria | [8] | | |
| restaurante_categoria | [9] | | |
| producto | [29] | | |
| pedido | [24] | | |
| detalle_pedido | [51] | | |
| calificacion | [14] | | |
| Resultado de C07 | [911500.00] | | |

**Evidencia 3:** conteos de las tablas en la base original.

![Conteos en la base original](evidencias/03_conteos_original.png)

**Evidencia 4:** resultado de C07 en la base original.

![C07 en la base original](evidencias/04_c07_original.png)

## 11. Restauración del respaldo

Se restauró el respaldo en una base nueva y vacía, sin tocar la base original, para comprobar que el archivo sirve.

| Dato | Valor |
|---|---|
| Fecha y hora | [24/11/2026, 12:44:05 a. m.] |
| Responsable | Ednilson Contreras |
| Archivo usado | `blinkdelivery_2026-11-23.backup` |
| Base destino | `blinkdelivery_restore` (creada vacía) |
| Formato | Custom or tar |
| Duración | 0,86 segundos |
| Resultado | Exitoso, sin errores en el registro |

La duración queda muy por debajo de la meta de recuperación (RTO) de 30 minutos. Es un tiempo corto porque la base es pequeña.

### Procedimiento

1. Clic derecho en **Databases → Create → Database...** y se creó `blinkdelivery_restore`.
2. Clic derecho sobre `blinkdelivery_restore` y se eligió **Restore...**
3. En **Format** se eligió **Custom or tar** y en **Filename** el archivo `.backup`.
4. Se presionó **Restore** y se verificó en More details que terminó con "Successfully completed".
5. Se actualizó el árbol (**Refresh**) y se comprobó que en **Schemas → public → Tables** aparecen las 11 tablas.

### Comando ejecutado por pgAdmin

```text
[pega aquí la línea "Running the command: ... pg_restore ..."]
```

### Evidencias

**Evidencia 5:** configuración de la restauración.

![Configuración de la restauración](evidencias/05_restore_configuracion.png)

**Evidencia 6:** restauración terminada con éxito.

![Restauración exitosa](evidencias/06_restore_exitoso.png)