```mermaid

erDiagram
    cliente ||--o{ direccion : "registra"
    cliente ||--o{ pedido : "realiza"
    cliente ||--o{ calificacion : "emite"
    direccion ||--o{ pedido : "es destino de"
    restaurante ||--o{ producto : "ofrece"
    restaurante ||--o{ pedido : "prepara"
    restaurante ||--o{ calificacion : "recibe"
    restaurante ||--o{ restaurante_categoria : "se clasifica en"
    categoria ||--o{ restaurante_categoria : "agrupa"
    domiciliario |o--o{ pedido : "entrega"
    pedido ||--|{ detalle_pedido : "contiene"
    producto ||--o{ detalle_pedido : "aparece en"
    pedido ||--o| calificacion : "habilita"

    cliente {
        serial id_cliente PK
        varchar nombre "60, no nulo"
        varchar apellido "60, no nulo"
        varchar correo "120, unico"
        varchar telefono "20, nulo"
        varchar contrasena_hash "255, cifrada"
        timestamp fecha_registro "no nulo"
    }

    direccion {
        serial id_direccion PK
        integer id_cliente FK "cliente.id_cliente"
        varchar alias "40, nulo - Casa, Trabajo"
        varchar direccion_texto "200, no nulo"
        varchar ciudad "60, no nulo"
        varchar referencia "150, nulo"
        boolean es_principal "no nulo"
    }

    restaurante {
        serial id_restaurante PK
        varchar nombre "100, no nulo"
        text descripcion "nulo"
        varchar telefono "20, nulo"
        varchar direccion_texto "200, no nulo"
        varchar ciudad "60, no nulo"
        varchar estado "20, abierto o cerrado"
        numeric calificacion_promedio "3-2, nulo - derivado"
        timestamp fecha_registro "no nulo"
    }

    categoria {
        serial id_categoria PK
        varchar nombre "50, no nulo"
    }

    restaurante_categoria {
        integer id_restaurante PK "FK restaurante"
        integer id_categoria PK "FK categoria"
    }

    producto {
        serial id_producto PK
        integer id_restaurante FK "restaurante.id_restaurante"
        varchar nombre "100, no nulo"
        text descripcion "nulo"
        numeric precio "10-2, no nulo"
        boolean disponible "no nulo"
    }

    domiciliario {
        serial id_domiciliario PK
        varchar nombre "60, no nulo"
        varchar apellido "60, no nulo"
        varchar telefono "20, no nulo"
        varchar estado "20, disponible o en entrega"
        timestamp fecha_registro "no nulo"
    }

    pedido {
        serial id_pedido PK
        integer id_cliente FK "cliente.id_cliente"
        integer id_restaurante FK "restaurante.id_restaurante"
        integer id_domiciliario FK "nulo hasta ser asignado"
        integer id_direccion FK "direccion.id_direccion"
        varchar estado "30, recibido a entregado"
        timestamp fecha_pedido "no nulo"
        numeric total "10-2, no nulo"
        text notas "nulo"
    }

    detalle_pedido {
        serial id_detalle_pedido PK
        integer id_pedido FK "pedido.id_pedido"
        integer id_producto FK "producto.id_producto"
        integer cantidad "no nulo"
        numeric precio_unitario "10-2, congelado en la compra"
    }

    calificacion {
        serial id_calificacion PK
        integer id_pedido FK,UK "un pedido se califica una vez"
        integer id_cliente FK "cliente.id_cliente"
        integer id_restaurante FK "restaurante.id_restaurante"
        smallint puntuacion "1 a 5"
        text comentario "nulo"
        timestamp fecha "no nulo"
    }
