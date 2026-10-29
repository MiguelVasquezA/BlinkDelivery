CREATE TABLE producto (
    id_producto INTEGER PRIMARY KEY,
    id_restaurante INTEGER NOT NULL
        REFERENCES restaurante (id_restaurante) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    precio NUMERIC(10,2) NOT NULL
        CONSTRAINT precio_positivo CHECK (precio > 0),
    disponible BOOLEAN NOT NULL DEFAULT TRUE
);