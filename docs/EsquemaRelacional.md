```mermaid

erDiagram

&#x20;   CLIENTE {

&#x20;       int id\_cliente PK

&#x20;       string nombre

&#x20;       string correo

&#x20;   }



&#x20;   TELEFONO\_CLIENTE {

&#x20;       int id\_telefono PK

&#x20;       int id\_cliente FK

&#x20;       string numero

&#x20;   }



&#x20;   RESTAURANTE {

&#x20;       int id\_restaurante PK

&#x20;       string nombre

&#x20;       string direccion

&#x20;       string telefono

&#x20;   }



&#x20;   PRODUCTO {

&#x20;       int id\_producto PK

&#x20;       int id\_restaurante FK

&#x20;       string nombre

&#x20;       decimal precio

&#x20;       string descripcion

&#x20;   }



&#x20;   DOMICILIARIO {

&#x20;       int id\_domiciliario PK

&#x20;       string nombre

&#x20;   }



&#x20;   TELEFONO\_DOMICILIARIO {

&#x20;       int id\_telefono PK

&#x20;       int id\_domiciliario FK

&#x20;       string numero

&#x20;   }



&#x20;   PEDIDO {

&#x20;       int id\_pedido PK

&#x20;       int id\_cliente FK

&#x20;       int id\_restaurante FK

&#x20;       int id\_domiciliario FK

&#x20;       timestamp fecha\_hora

&#x20;       string estado

&#x20;       string direccion\_entrega

&#x20;   }



&#x20;   DETALLE\_PEDIDO {

&#x20;       int id\_detalle PK

&#x20;       int id\_pedido FK

&#x20;       int id\_producto FK

&#x20;       int cantidad

&#x20;       decimal precio\_unitario

&#x20;   }



&#x20;   CLIENTE ||--o{ TELEFONO\_CLIENTE : tiene

&#x20;   CLIENTE ||--o{ PEDIDO : realiza

&#x20;   RESTAURANTE ||--o{ PRODUCTO : ofrece

&#x20;   RESTAURANTE ||--o{ PEDIDO : recibe

&#x20;   DOMICILIARIO ||--o{ TELEFONO\_DOMICILIARIO : tiene

&#x20;   DOMICILIARIO ||--o{ PEDIDO : entrega

&#x20;   PEDIDO ||--|{ DETALLE\_PEDIDO : contiene

&#x20;   PRODUCTO ||--o{ DETALLE\_PEDIDO : incluye

