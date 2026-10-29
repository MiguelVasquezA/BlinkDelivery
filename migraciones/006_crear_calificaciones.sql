CREATE TABLE calificacion (
    id_calificacion INTEGER PRIMARY KEY,
    id_pedido INTEGER NOT NULL
        CONSTRAINT calificacion_por_pedido_unica UNIQUE
        REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    id_cliente INTEGER NOT NULL
        REFERENCES cliente (id_cliente) ON DELETE RESTRICT,
    id_restaurante INTEGER NOT NULL
        REFERENCES restaurante (id_restaurante) ON DELETE RESTRICT,
    puntuacion INTEGER NOT NULL
        CONSTRAINT puntuacion_en_escala
        CHECK (puntuacion >= 1 AND puntuacion <= 5),
    comentario VARCHAR(200),
    fecha DATE NOT NULL
);