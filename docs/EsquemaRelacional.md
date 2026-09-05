```mermaid

erDiagram

&#x20;   CLIENTE {

&#x20;       int id_cliente PK

&#x20;       string nombre

&#x20;       string correo

&#x20;   }



&#x20;   TELEFONO_CLIENTE {

&#x20;       int id_telefono PK

&#x20;       int id_cliente FK

&#x20;       string numero

&#x20;   }



&#x20;   RESTAURANTE {

&#x20;       int id_restaurante PK

&#x20;       string nombre

&#x20;       string direccion

&#x20;       string telefono

&#x20;   }



&#x20;   PRODUCTO {

&#x20;       int id_producto PK

&#x20;       int id_restaurante FK

&#x20;       string nombre

&#x20;       decimal precio

&#x20;       string descripcion

&#x20;   }



&#x20;   DOMICILIARIO {

&#x20;       int id_domiciliario PK

&#x20;       string nombre

&#x20;   }



&#x20;   TELEFONO_DOMICILIARIO {

&#x20;       int id_telefono PK

&#x20;       int id_domiciliario FK

&#x20;       string numero

&#x20;   }



&#x20;   PEDIDO {

&#x20;       int id_pedido PK

&#x20;       int id_cliente FK

&#x20;       int id_restaurante FK

&#x20;       int id_domiciliario FK

&#x20;       timestamp fecha_hora

&#x20;       string estado

&#x20;       string direccion_entrega

&#x20;   }



&#x20;   DETALLE_PEDIDO {

&#x20;       int id_detalle PK

&#x20;       int id_pedido FK

&#x20;       int id_producto FK

&#x20;       int cantidad

&#x20;       decimal precio_unitario

&#x20;   }



&#x20;   CLIENTE ||--o{ TELEFONO_CLIENTE : tiene

&#x20;   CLIENTE ||--o{ PEDIDO : realiza

&#x20;   RESTAURANTE ||--o{ PRODUCTO : ofrece

&#x20;   RESTAURANTE ||--o{ PEDIDO : recibe

&#x20;   DOMICILIARIO ||--o{ TELEFONO_DOMICILIARIO : tiene

&#x20;   DOMICILIARIO ||--o{ PEDIDO : entrega

&#x20;   PEDIDO ||--|{ DETALLE_PEDIDO : contiene

&#x20;   PRODUCTO ||--o{ DETALLE_PEDIDO : incluye

