
# Reglas pendientes

Reglas de negocio que no se pueden expresar con restricciones declarativas,
porque dependen de otras filas o de otras tablas. Se protegerán en la Entrega 3.

| Regla | Por qué no cabe en un CHECK | Cómo se resolverá |
|---|---|---|
| RN09: un pedido solo tiene productos de su mismo restaurante | Compara detalle_pedido con producto y pedido | Trigger de validación |
| RN12: solo se califica un restaurante donde se compró | Compara calificacion con pedido | Trigger de validación |
| RN25: no hay personas sin rol | Exige una fila en cliente o en domiciliario | Trigger o función |
| RN26: el domiciliario debe tener teléfono | El teléfono está en persona y el rol en domiciliario | Trigger de validación |
| RN27: el pedido va a una dirección de su mismo cliente | Compara pedido con direccion | Trigger de validación |
| Una sola dirección principal por cliente | Compara varias filas de direccion | Trigger |
| El total del pedido es la suma de su detalle | Compara pedido con detalle_pedido | Función o trigger |
| El promedio del restaurante coincide con sus calificaciones | Es un dato derivado de otra tabla | Trigger |
