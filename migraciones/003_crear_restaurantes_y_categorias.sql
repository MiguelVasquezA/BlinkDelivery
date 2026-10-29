CREATE TABLE restaurante (
    id_restaurante INTEGER PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    telefono VARCHAR(20),
    direccion_texto VARCHAR(200) NOT NULL,
    ciudad VARCHAR(60) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'cerrado'
        CONSTRAINT estado_restaurante_valido
        CHECK (estado = 'abierto' OR estado = 'cerrado'),
    calificacion_promedio NUMERIC(3,2)
        CONSTRAINT promedio_en_escala
        CHECK (
            calificacion_promedio >= 1
            AND calificacion_promedio <= 5
        ),
    fecha_registro DATE NOT NULL
);

CREATE TABLE categoria (
    id_categoria INTEGER PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
        CONSTRAINT nombre_categoria_unico UNIQUE
);

CREATE TABLE restaurante_categoria (
    id_restaurante INTEGER NOT NULL
        REFERENCES restaurante (id_restaurante) ON DELETE CASCADE,
    id_categoria INTEGER NOT NULL
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT,
    PRIMARY KEY (id_restaurante, id_categoria)
);