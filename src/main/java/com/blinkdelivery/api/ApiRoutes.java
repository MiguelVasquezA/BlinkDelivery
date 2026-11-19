package com.blinkdelivery.api;

import com.blinkdelivery.dao.AddresFileDao;
import com.blinkdelivery.dao.CredencialFileDao;
import com.blinkdelivery.dao.CategoryFileDao;
import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.OrderFileDao;
import com.blinkdelivery.dao.OrderStatusHistoryFileDao;
import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RatingFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Category;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatusChange;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Rating;
import com.blinkdelivery.modelo.Restaurant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Enrutamiento de la API REST. Cada ruta llama directamente a los métodos de los DAO
 * existentes y convierte el resultado a mapas que {@link Json} serializa.
 *
 * <pre>
 * GET    /api/health
 * GET    /api/customers                      POST /api/customers
 * GET    /api/customers/{id}                 PUT  /api/customers/{id}
 * GET    /api/customers/{id}/addresses       POST /api/customers/{id}/addresses
 * DELETE /api/addresses/{id}
 * GET    /api/restaurants                    POST /api/restaurants
 * GET    /api/restaurants/{id}               PUT  /api/restaurants/{id}
 * GET    /api/restaurants/{id}/categories    POST /api/restaurants/{id}/categories
 * DELETE /api/restaurants/{id}/categories/{categoryId}
 * GET    /api/products                       POST /api/products
 * GET    /api/products/{id}                  PUT  /api/products/{id}
 * GET    /api/categories                     POST /api/categories
 * GET    /api/categories/{id}
 * GET    /api/drivers                        POST /api/drivers
 * GET    /api/drivers/{id}                   PUT  /api/drivers/{id}
 * GET    /api/orders                         GET  /api/orders/{id}
 * GET    /api/orders/{id}/history
 * GET    /api/ratings                        POST /api/ratings
 * </pre>
 *
 * Los pedidos no se crean por esta API: un {@code Order} requiere reglas de negocio de
 * {@code OrderService}, que este módulo no expone todavía.
 */
final class ApiRoutes {

    /** Resultado de una petición: código HTTP y cuerpo a serializar. */
    record Respuesta(int estado, Object cuerpo) {
    }

    /** Error controlado que se traduce a un código HTTP concreto. */
    static final class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int estado;

        ApiException(int estado, String mensaje) {
            super(mensaje);
            this.estado = estado;
        }
    }

    private final CustomerFileDao clientes = new CustomerFileDao();
    private final AddresFileDao direcciones = new AddresFileDao();
    private final RestaurantFileDao restaurantes = new RestaurantFileDao();
    private final CategoryFileDao categorias = new CategoryFileDao();
    private final ProductFileDao productos = new ProductFileDao();
    private final DriverFileDao domiciliarios = new DriverFileDao();
    private final OrderFileDao pedidos = new OrderFileDao();
    private final OrderStatusHistoryFileDao historial = new OrderStatusHistoryFileDao();
    private final RatingFileDao calificaciones = new RatingFileDao();
    private final CredencialFileDao credenciales = new CredencialFileDao();

    /**
     * @param metodo método HTTP en mayúsculas
     * @param seg    segmentos de la ruta después de {@code /api}
     * @param cuerpo cuerpo JSON de la petición (puede ser vacío)
     */
    Respuesta manejar(String metodo, List<String> seg, String cuerpo) throws IOException {
        if (seg.isEmpty()) {
            throw noEncontrado();
        }
        switch (seg.get(0)) {
            case "health":
                if (!metodo.equals("GET")) throw noPermitido();
                return ok(Map.of("estado", "ok"));
            case "customers":
                return clientes(metodo, seg, cuerpo);
            case "addresses":
                return direccion(metodo, seg);
            case "restaurants":
                return restaurantes(metodo, seg, cuerpo);
            case "products":
                return productos(metodo, seg, cuerpo);
            case "categories":
                return categoriasRuta(metodo, seg, cuerpo);
            case "drivers":
                return domiciliarios(metodo, seg, cuerpo);
            case "orders":
                return pedidosRuta(metodo, seg);
            case "ratings":
                return calificacionesRuta(metodo, seg, cuerpo);
            case "auth":
                return autenticacion(metodo, seg, cuerpo);
            default:
                throw noEncontrado();
        }
    }

    // ------------------------------------------------------------ autenticación

    /**
     * POST /api/auth/login  {rol, usuario, clave}: valida la contraseña del perfil.
     * POST /api/auth/clave  {rol, usuario, clave}: establece la contraseña de un perfil existente que aún no la tiene.
     * El rol es "cliente", "restaurante" o "domiciliario"; el usuario es la identificación (o el ID del restaurante).
     */
    private Respuesta autenticacion(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() != 2) throw noEncontrado();
        if (!m.equals("POST")) throw noPermitido();

        Map<String, Object> d = Json.leerObjeto(cuerpo);
        String rol = texto(d, "rol", true);
        String usuario = texto(d, "usuario", true).trim();
        String clave = texto(d, "clave", true);
        if (usuario.isEmpty() || usuario.contains(";")) {
            throw new IllegalArgumentException("El usuario no es válido.");
        }

        Map<String, Object> perfil = perfilDe(rol, usuario);
        if (perfil == null) throw new ApiException(404, "No existe un perfil con ese ID.");
        String[] cuenta = credenciales.buscar(rol, usuario);

        switch (seg.get(1)) {
            case "login":
                if (cuenta == null) {
                    throw new ApiException(409, "Este perfil aún no tiene contraseña. Usa «Crear cuenta» para establecerla.");
                }
                if (!Autenticacion.verificar(clave, cuenta[0], cuenta[1])) {
                    throw new ApiException(401, "Usuario o contraseña incorrectos.");
                }
                return ok(perfil);
            case "clave":
                if (clave.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
                if (cuenta != null) throw conflicto("Este perfil ya tiene contraseña. Inicia sesión.");
                String[] nueva = Autenticacion.cifrar(clave);
                credenciales.guardar(rol, usuario, nueva[0], nueva[1]);
                return creado(Map.of("ok", true));
            default:
                throw noEncontrado();
        }
    }

    /** Perfil del rol indicado, o {@code null} si no existe. */
    private Map<String, Object> perfilDe(String rol, String id) throws IOException {
        switch (rol) {
            case "cliente":
                Customer c = clientes.buscarPorId(id);
                return c == null ? null : aMapa(c);
            case "restaurante":
                Restaurant r = restaurantes.buscarPorId(id);
                return r == null ? null : aMapa(r);
            case "domiciliario":
                DeliveryDriver dom = domiciliarios.buscarPorId(id);
                return dom == null ? null : aMapa(dom);
            default:
                throw new IllegalArgumentException("Rol no válido: '" + rol + "'.");
        }
    }

    // ------------------------------------------------------------ clientes

    private Respuesta clientes(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(clientes.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "identificacion", true);
                if (clientes.buscarPorId(id) != null) throw conflicto("Ya existe un cliente con identificacion " + id + ".");
                Customer c = new Customer(id, texto(d, "nombre", true), texto(d, "telefono", true),
                        texto(d, "direccion", true));
                String email = texto(d, "email", false);
                if (email != null) c.setEmail(email);
                clientes.guardar(c);
                return creado(aMapa(c));
            }
            throw noPermitido();
        }
        Customer actual = clientes.buscarPorId(seg.get(1));
        if (seg.size() == 2) {
            if (m.equals("GET")) return ok(aMapa(existe(actual, "cliente", seg.get(1))));
            if (m.equals("PUT")) {
                existe(actual, "cliente", seg.get(1));
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String nombre = texto(d, "nombre", false);
                String telefono = texto(d, "telefono", false);
                String direccion = texto(d, "direccion", false);
                String email = texto(d, "email", false);
                if (nombre != null) actual.setNombre(nombre);
                if (telefono != null) actual.setTelefono(telefono);
                if (direccion != null) actual.setAddress(direccion);
                if (email != null) actual.setEmail(email);
                clientes.actualizar(actual);
                return ok(aMapa(actual));
            }
            throw noPermitido();
        }
        if (seg.size() == 3 && seg.get(2).equals("addresses")) {
            existe(actual, "cliente", seg.get(1));
            if (m.equals("GET")) return ok(mapear(direcciones.listarPorCliente(actual.getIdentification()), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                Address nueva = new Address(texto(d, "id", true), texto(d, "etiqueta", true),
                        texto(d, "direccionExacta", true), texto(d, "barrio", false),
                        texto(d, "ciudad", false), texto(d, "referencia", false));
                direcciones.guardar(actual.getIdentification(), nueva);
                return creado(aMapa(nueva));
            }
            throw noPermitido();
        }
        throw noEncontrado();
    }

    private Respuesta direccion(String m, List<String> seg) throws IOException {
        if (seg.size() != 2) throw noEncontrado();
        if (!m.equals("DELETE")) throw noPermitido();
        boolean eliminada = direcciones.eliminar(seg.get(1));
        if (!eliminada) throw new ApiException(404, "No existe la direccion " + seg.get(1) + ".");
        return ok(Map.of("eliminada", true));
    }

    // ------------------------------------------------------------ restaurantes

    private Respuesta restaurantes(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(restaurantes.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "id", true);
                if (restaurantes.buscarPorId(id) != null) throw conflicto("Ya existe un restaurante con id " + id + ".");
                Restaurant r = new Restaurant(id, texto(d, "nombre", true), texto(d, "tipoCocina", true));
                String direccion = texto(d, "direccion", false);
                String telefono = texto(d, "telefono", false);
                Boolean abierto = booleano(d, "abierto");
                if (direccion != null) r.setDireccion(direccion);
                if (telefono != null) r.setTelefono(telefono);
                if (abierto != null) r.setOpen(abierto);
                restaurantes.guardar(r);
                return creado(aMapa(r));
            }
            throw noPermitido();
        }
        Restaurant actual = restaurantes.buscarPorId(seg.get(1));
        if (seg.size() == 2) {
            if (m.equals("GET")) return ok(aMapa(existe(actual, "restaurante", seg.get(1))));
            if (m.equals("PUT")) {
                existe(actual, "restaurante", seg.get(1));
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String nombre = texto(d, "nombre", false);
                String tipo = texto(d, "tipoCocina", false);
                String direccion = texto(d, "direccion", false);
                String telefono = texto(d, "telefono", false);
                Boolean abierto = booleano(d, "abierto");
                if (nombre != null) actual.setNombre(nombre);
                if (tipo != null) actual.setTipoCocina(tipo);
                if (direccion != null) actual.setDireccion(direccion);
                if (telefono != null) actual.setTelefono(telefono);
                if (abierto != null) actual.setOpen(abierto);
                restaurantes.actualizar(actual);
                return ok(aMapa(actual));
            }
            throw noPermitido();
        }
        if (seg.size() >= 3 && seg.get(2).equals("categories")) {
            existe(actual, "restaurante", seg.get(1));
            if (seg.size() == 3) {
                if (m.equals("GET")) return ok(mapear(categorias.listarPorRestaurante(actual.getId()), ApiRoutes::aMapa));
                if (m.equals("POST")) {
                    Map<String, Object> d = Json.leerObjeto(cuerpo);
                    String idCategoria = texto(d, "categoriaId", true);
                    existe(categorias.buscarPorId(idCategoria), "categoria", idCategoria);
                    categorias.vincular(actual.getId(), idCategoria);
                    return creado(Map.of("restauranteId", actual.getId(), "categoriaId", idCategoria));
                }
                throw noPermitido();
            }
            if (seg.size() == 4) {
                if (!m.equals("DELETE")) throw noPermitido();
                boolean quitada = categorias.desvincular(actual.getId(), seg.get(3));
                if (!quitada) throw new ApiException(404, "El restaurante no tiene la categoria " + seg.get(3) + ".");
                return ok(Map.of("desvinculada", true));
            }
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ productos

    private Respuesta productos(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(productos.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "id", true);
                String restauranteId = texto(d, "restauranteId", true);
                if (productos.buscarPorId(id) != null) throw conflicto("Ya existe un producto con id " + id + ".");
                existe(restaurantes.buscarPorId(restauranteId), "restaurante", restauranteId);
                Product p = new Product(id, restauranteId, texto(d, "nombre", true),
                        texto(d, "descripcion", true), numero(d, "precio"));
                Boolean disponible = booleano(d, "disponible");
                if (disponible != null) p.setAvailable(disponible);
                productos.guardar(p);
                return creado(aMapa(p));
            }
            throw noPermitido();
        }
        if (seg.size() == 2) {
            Product actual = productos.buscarPorId(seg.get(1));
            if (m.equals("GET")) return ok(aMapa(existe(actual, "producto", seg.get(1))));
            if (m.equals("PUT")) {
                existe(actual, "producto", seg.get(1));
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String nombre = texto(d, "nombre", false);
                String descripcion = texto(d, "descripcion", false);
                Double precio = numeroOpcional(d, "precio");
                Boolean disponible = booleano(d, "disponible");
                if (nombre != null) actual.setNombre(nombre);
                if (descripcion != null) actual.setDescripcion(descripcion);
                if (precio != null) actual.setPrecio(precio);
                if (disponible != null) actual.setDisponible(disponible);
                productos.actualizar(actual);
                return ok(aMapa(actual));
            }
            throw noPermitido();
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ categorías

    private Respuesta categoriasRuta(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(categorias.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "id", true);
                if (categorias.buscarPorId(id) != null) throw conflicto("Ya existe una categoria con id " + id + ".");
                Category c = new Category(id, texto(d, "nombre", true), texto(d, "descripcion", true));
                categorias.guardar(c);
                return creado(aMapa(c));
            }
            throw noPermitido();
        }
        if (seg.size() == 2) {
            if (!m.equals("GET")) throw noPermitido(); // CategoryFileDao no tiene actualizar
            return ok(aMapa(existe(categorias.buscarPorId(seg.get(1)), "categoria", seg.get(1))));
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ domiciliarios

    private Respuesta domiciliarios(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(domiciliarios.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "identificacion", true);
                if (domiciliarios.buscarPorId(id) != null) throw conflicto("Ya existe un domiciliario con identificacion " + id + ".");
                DeliveryDriver dom = new DeliveryDriver(id, texto(d, "nombre", true),
                        texto(d, "telefono", true), texto(d, "tipoVehiculo", true));
                domiciliarios.guardar(dom);
                return creado(aMapa(dom));
            }
            throw noPermitido();
        }
        if (seg.size() == 2) {
            DeliveryDriver actual = domiciliarios.buscarPorId(seg.get(1));
            if (m.equals("GET")) return ok(aMapa(existe(actual, "domiciliario", seg.get(1))));
            if (m.equals("PUT")) {
                existe(actual, "domiciliario", seg.get(1));
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String nombre = texto(d, "nombre", false);
                String telefono = texto(d, "telefono", false);
                String tipo = texto(d, "tipoVehiculo", false);
                String estado = texto(d, "estado", false);
                if (nombre != null) actual.setNombre(nombre);
                if (telefono != null) actual.setTelefono(telefono);
                if (tipo != null) actual.setTipoVehiculo(tipo);
                if (estado != null) actual.setEstado(DriverStatus.desdeTexto(estado));
                domiciliarios.actualizar(actual);
                return ok(aMapa(actual));
            }
            throw noPermitido();
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ pedidos (solo lectura)

    private Respuesta pedidosRuta(String m, List<String> seg) throws IOException {
        if (!m.equals("GET")) throw noPermitido();
        if (seg.size() == 1) return ok(mapear(pedidos.listar(), ApiRoutes::aMapa));
        if (seg.size() == 2) return ok(aMapa(existe(pedidos.buscarPorId(seg.get(1)), "pedido", seg.get(1))));
        if (seg.size() == 3 && seg.get(2).equals("history")) {
            existe(pedidos.buscarPorId(seg.get(1)), "pedido", seg.get(1));
            return ok(mapear(historial.listarPorPedido(seg.get(1)), ApiRoutes::aMapa));
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ calificaciones

    private Respuesta calificacionesRuta(String m, List<String> seg, String cuerpo) throws IOException {
        if (seg.size() == 1) {
            if (m.equals("GET")) return ok(mapear(calificaciones.listar(), ApiRoutes::aMapa));
            if (m.equals("POST")) {
                Map<String, Object> d = Json.leerObjeto(cuerpo);
                String id = texto(d, "id", true);
                if (calificaciones.listar().stream().anyMatch(r -> r.getId().equalsIgnoreCase(id))) {
                    throw conflicto("Ya existe una calificacion con id " + id + ".");
                }
                double estrellas = numero(d, "estrellas");
                if (estrellas != Math.floor(estrellas)) {
                    throw new IllegalArgumentException("Las estrellas deben ser un número entero.");
                }
                Rating r = new Rating(id, texto(d, "pedidoId", true), texto(d, "clienteId", true),
                        texto(d, "restauranteId", true), (int) estrellas, texto(d, "comentario", false));
                calificaciones.guardar(r);
                return creado(aMapa(r));
            }
            throw noPermitido();
        }
        throw noEncontrado();
    }

    // ------------------------------------------------------------ mapeo modelo -> mapa

    private static Map<String, Object> aMapa(Customer c) {
        return campos("identificacion", c.getIdentification(), "nombre", c.getNombre(),
                "telefono", c.getTelefono(), "email", c.getEmail(), "direccion", c.getAddress());
    }

    private static Map<String, Object> aMapa(Address a) {
        return campos("id", a.getId(), "etiqueta", a.getEtiqueta(), "direccionExacta", a.getDireccionExacta(),
                "barrio", a.getBarrio(), "ciudad", a.getCiudad(), "referencia", a.getReferencia());
    }

    private static Map<String, Object> aMapa(Restaurant r) {
        return campos("id", r.getId(), "nombre", r.getNombre(), "tipoCocina", r.getTipoCocina(),
                "direccion", r.getDireccion(), "telefono", r.getTelefono(),
                "horaApertura", String.valueOf(r.getHoraApertura()), "horaCierre", String.valueOf(r.getHoraCierre()),
                "abierto", r.estaAbierto(), "promedioCalificacion", r.getPromedioCalificacion());
    }

    private static Map<String, Object> aMapa(Product p) {
        Category c = p.getCategoria();
        return campos("id", p.getId(), "restauranteId", p.getRestaurantId(), "nombre", p.getNombre(),
                "descripcion", p.getDescripcion(), "precio", p.getPrecio(), "disponible", p.isDisponible(),
                "categoria", c == null ? null : c.getNombre());
    }

    private static Map<String, Object> aMapa(Category c) {
        return campos("id", c.getId(), "nombre", c.getNombre(), "descripcion", c.getDescripcion());
    }

    private static Map<String, Object> aMapa(DeliveryDriver d) {
        return campos("identificacion", d.getIdentification(), "nombre", d.getNombre(),
                "telefono", d.getTelefono(), "tipoVehiculo", d.getTipoVehiculo(),
                "estado", d.getEstado().name(), "calificacionPromedio", d.getCalificacionPromedio());
    }

    private static Map<String, Object> aMapa(Order o) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem it : o.getItems()) {
            items.add(campos("productoId", it.getProducto().getId(), "nombre", it.getProducto().getNombre(),
                    "cantidad", it.getCantidad(), "subtotal", it.getSubtotal()));
        }
        Address entrega = o.getDireccionEntrega();
        return campos("id", o.getId(), "clienteId", o.getCustomerId(), "restauranteId", o.getRestaurantId(),
                "estado", o.getEstado().name(), "fechaHora", String.valueOf(o.getFechaHora()),
                "direccionEntrega", entrega == null ? null : entrega.getDireccionExacta(),
                "items", items, "subtotal", o.getSubtotal(), "comisionPlataforma", o.getComisionPlataforma(),
                "total", o.getTotal());
    }

    private static Map<String, Object> aMapa(Rating r) {
        return campos("id", r.getId(), "pedidoId", r.getOrderId(), "clienteId", r.getCustomerId(),
                "restauranteId", r.getRestaurantId(), "estrellas", r.getEstrellas(),
                "comentario", r.getComentario(), "fecha", String.valueOf(r.getFecha()));
    }

    private static Map<String, Object> aMapa(OrderStatusChange c) {
        // OrderStatusChange no expone getFecha(); su toRegistro() es "idPedido;estado;fecha".
        String fecha = c.toRegistro().split(";", -1)[2];
        return campos("pedidoId", c.getOrderId(), "estado", c.getEstado().name(), "fecha", fecha);
    }

    // ------------------------------------------------------------ utilidades

    /** Construye un mapa ordenado a partir de pares clave, valor. */
    private static Map<String, Object> campos(Object... kv) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            mapa.put((String) kv[i], kv[i + 1]);
        }
        return mapa;
    }

    private static <T> List<Map<String, Object>> mapear(List<T> origen, Function<T, Map<String, Object>> f) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (T t : origen) {
            salida.add(f.apply(t));
        }
        return salida;
    }

    private static <T> T existe(T valor, String nombre, String id) {
        if (valor == null) {
            throw new ApiException(404, "No existe el " + nombre + " con id " + id + ".");
        }
        return valor;
    }

    private static String texto(Map<String, Object> d, String clave, boolean obligatorio) {
        Object v = d.get(clave);
        if (v == null) {
            if (obligatorio) throw new IllegalArgumentException("Falta el campo '" + clave + "'.");
            return null;
        }
        if (!(v instanceof String s)) throw new IllegalArgumentException("El campo '" + clave + "' debe ser texto.");
        return s;
    }

    private static double numero(Map<String, Object> d, String clave) {
        Double v = numeroOpcional(d, clave);
        if (v == null) throw new IllegalArgumentException("Falta el campo '" + clave + "'.");
        return v;
    }

    private static Double numeroOpcional(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        if (v == null) return null;
        if (!(v instanceof Double n)) throw new IllegalArgumentException("El campo '" + clave + "' debe ser un número.");
        return n;
    }

    private static Boolean booleano(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        if (v == null) return null;
        if (!(v instanceof Boolean b)) throw new IllegalArgumentException("El campo '" + clave + "' debe ser true o false.");
        return b;
    }

    private static Respuesta ok(Object cuerpo) {
        return new Respuesta(200, cuerpo);
    }

    private static Respuesta creado(Object cuerpo) {
        return new Respuesta(201, cuerpo);
    }

    private static ApiException noPermitido() {
        return new ApiException(405, "Método no permitido en esta ruta.");
    }

    private static ApiException noEncontrado() {
        return new ApiException(404, "Ruta no encontrada.");
    }

    private static ApiException conflicto(String mensaje) {
        return new ApiException(409, mensaje);
    }
}
