# Sistema de Gestión de Domicilios

Proyecto Integrador de la asignatura **Base de Datos I**.

## Integrantes

- Miguel Angel Vasquez Aguirre (Grupo 05)
- Adriana Dangond Cantillo (Grupo 06)
- Ednilson Contreras (Grupo 05)
- Alejandro Enrique Ochoa Sivera (Grupo 05)
- Iván Camilo Hernández (Grupo 07)
- Abraham Medina Rodriguez (Grupo 06)

## Descripción

Sistema de información para la gestión de domicilios que permite administrar clientes, direcciones, restaurantes, categorías, productos, domiciliarios, pedidos y calificaciones.

## Tecnologías y herramientas utilizadas

- Git
- GitHub
- Mermaid
- Microsoft Excel
- SVG

## Estructura del repositorio

```text
docs/
2
├── 01_Planteamiento_Problema.md
3
├── 02_Requisitos_Preguntas_Supuestos.md
4
├── 03_Diagrama_EER.svg
5
├── 04_Esquema_Relacional.md
6
├── 05_Diccionario_Datos.xlsx
```

La carpeta `docs` contiene:

- Planteamiento del problema
- Requisitos, preguntas y supuestos
- Diagrama EER
- Esquema relacional
- Diccionario de datos inicial


## Entrega 1

La entrega contiene:

- Planteamiento del problema
- Requisitos e interrogatorio
- Diagrama EER
- Esquema relacional
- Diccionario de datos inicial

## Control de versiones

Etiqueta de la entrega:

```text
entrega-1
```

## Migraciones

La carpeta `migraciones/` contiene el esquema de la base de datos en scripts numerados.

Para reconstruir la base de datos desde cero:

1. Crear una base de datos vacía en PostgreSQL.
2. Ejecutar los archivos en orden:
   - `001_crear_personas_y_roles.sql`
   - `002_crear_direcciones.sql`
   - `003_crear_restaurantes_y_categorias.sql`
   - `004_crear_productos.sql`
   - `005_crear_pedidos.sql`
   - `006_crear_calificaciones.sql`

Los scripts deben ejecutarse en orden, porque algunas tablas
dependen de otras mediante claves foráneas.

La carpeta `docs/` contiene la documentación del proyecto,
incluido `ReglasPendientes.md`, que registra las reglas de negocio
que se abordarán en la Entrega 3.
