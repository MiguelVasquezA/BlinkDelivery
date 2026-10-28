CREATE TABLE pedido (
    id_pedido INTEGER PRIMARY KEY,
    id_cliente INTEGER NOT NULL
        REFERENCES cliente (id_cliente) ON DELETE RESTRICT,
    id_restaurante INTEGER NOT NULL
        REFERENCES restaurante (id_restaurante) ON DELETE RESTRICT,
    id_domiciliario INTEGER
        REFERENCES domiciliario (id_domiciliario) ON DELETE RESTRICT,
    id_direccion INTEGER NOT NULL
        REFERENCES direccion (id_direccion) ON DELETE RESTRICT,
    estado VARCHAR(30) NOT NULL DEFAULT 'recibido'
        CONSTRAINT estado_pedido_valido
        CHECK (
            estado = 'recibido'
            OR estado = 'aceptado'
            OR estado = 'en preparación'
            OR estado = 'asignado'
            OR estado = 'en camino'
            OR estado = 'entregado'
            OR estado = 'cancelado'
        ),
    fecha_pedido TIMESTAMP NOT NULL,
    total NUMERIC(10,2) NOT NULL
        CONSTRAINT total_no_negativo CHECK (total >= 0),
    notas TEXT
);

CREATE TABLE detalle_pedido (
    id_detalle_pedido INTEGER PRIMARY KEY,
    id_pedido INTEGER NOT NULL
        REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    id_producto INTEGER NOT NULL
        REFERENCES producto (id_producto) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL
        CONSTRAINT cantidad_positiva CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL
        CONSTRAINT precio_unitario_positivo
        CHECK (precio_unitario > 0)
);