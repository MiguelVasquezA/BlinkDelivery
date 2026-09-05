Cliente {
    id_cliente,
    nombre,
    correo,
    direccion_calle,
    direccion_ciudad
}

Telefono_cliente {
    id_cliente,
    telefono
}

Restaurante {
    id_restaurante,
    nombre,
    nit
}

Producto {
    id_producto,
    nombre,
    precio,
    disponible,
    id_restaurante
}

Domiciliario {
    id_domiciliario,
    nombre,
    vehiculo
}

Telefono_domiciliario {
    id_domiciliario,
    telefono
}

Pedido {
    id_pedido,
    fecha,
    estado,
    id_cliente,
    id_restaurante,
    id_domiciliario
}