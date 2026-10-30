package com.blinkdelivery.negocio;

import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para la gestión de restaurantes y su catálogo de productos.
 * <p>
 * Permite registrar, consultar y actualizar restaurantes, abrirlos o cerrarlos,
 * y administrar los productos que ofrece cada uno.
 * </p>
 */
public class RestaurantService {

    /** DAO que guarda y lee los restaurantes en el archivo de texto. */
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();

    /** DAO que guarda y lee los productos en el archivo de texto. */
    private final ProductFileDao productDao = new ProductFileDao();

    /**
     * Registra un nuevo restaurante, que queda abierto por defecto.
     *
     * @param id        identificador del restaurante (debe ser único)
     * @param nombre    nombre del restaurante
     * @param categoria categoría de comida (por ejemplo, pizzas o hamburguesas)
     * @return el restaurante registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya existe un restaurante con ese id
     */
    public Restaurant registrarRestaurante(String id, String nombre, String categoria) throws IOException {
        id = texto(id, "El id del restaurante");
        nombre = texto(nombre, "El nombre");
        categoria = texto(categoria, "La categoría");

        if (buscarPorId(id) != null) {
            throw new IllegalStateException("Ya existe un restaurante con el id " + id + ".");
        }

        Restaurant restaurante = new Restaurant(id, nombre, categoria);
        restaurantDao.guardar(restaurante);
        return restaurante;
    }

    /**
     * Actualiza el nombre y la categoría de un restaurante existente.
     *
     * @param id        identificador del restaurante a actualizar
     * @param nombre    nuevo nombre
     * @param categoria nueva categoría
     * @return el restaurante con los datos actualizados
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el restaurante no existe o algún dato es inválido
     */
    public Restaurant actualizarRestaurante(String id, String nombre, String categoria) throws IOException {
        Restaurant restaurante = obtenerPorId(id);
        restaurante.setName(texto(nombre, "El nombre"));
        restaurante.setCategory(texto(categoria, "La categoría"));
        restaurantDao.actualizar(restaurante);
        return restaurante;
    }

    /**
     * Lista todos los restaurantes registrados.
     *
     * @return lista de restaurantes (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Restaurant> listarRestaurantes() throws IOException {
        return restaurantDao.listar();
    }

    /**
     * Lista los restaurantes que están abiertos y reciben pedidos.
     *
     * @return lista de restaurantes abiertos (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Restaurant> listarAbiertos() throws IOException {
        List<Restaurant> abiertos = new ArrayList<>();
        for (Restaurant r : restaurantDao.listar()) {
            if (r.isOpen()) {
                abiertos.add(r);
            }
        }
        return abiertos;
    }

    /**
     * Lista los restaurantes de una categoría, sin distinguir mayúsculas de minúsculas.
     *
     * @param categoria categoría a buscar
     * @return lista de restaurantes de esa categoría (vacía si no hay coincidencias)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si la categoría es vacía
     */
    public List<Restaurant> listarPorCategoria(String categoria) throws IOException {
        String buscada = texto(categoria, "La categoría");
        List<Restaurant> resultado = new ArrayList<>();
        for (Restaurant r : restaurantDao.listar()) {
            if (r.getCategory().equalsIgnoreCase(buscada)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * Busca restaurantes cuyo nombre contenga el texto indicado, sin distinguir
     * mayúsculas de minúsculas.
     *
     * @param texto texto a buscar dentro del nombre
     * @return lista de restaurantes que coinciden (vacía si no hay coincidencias)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el texto es vacío
     */
    public List<Restaurant> buscarPorNombre(String texto) throws IOException {
        String buscado = texto(texto, "El texto de búsqueda").toLowerCase();
        List<Restaurant> resultado = new ArrayList<>();
        for (Restaurant r : restaurantDao.listar()) {
            if (r.getName().toLowerCase().contains(buscado)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * Busca un restaurante por su identificador.
     *
     * @param id identificador a buscar
     * @return el restaurante encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Restaurant buscarPorId(String id) throws IOException {
        if (id == null) {
            return null;
        }
        String buscado = id.trim();
        for (Restaurant r : restaurantDao.listar()) {
            if (r.getId().equals(buscado)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Obtiene un restaurante por su identificador y falla si no existe.
     *
     * @param id identificador a buscar
     * @return el restaurante encontrado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe un restaurante con ese id
     */
    public Restaurant obtenerPorId(String id) throws IOException {
        Restaurant restaurante = buscarPorId(id);
        if (restaurante == null) {
            throw new IllegalArgumentException("No existe un restaurante con el id " + id + ".");
        }
        return restaurante;
    }

    /**
     * Abre o cierra un restaurante.
     *
     * @param id     identificador del restaurante
     * @param abierto {@code true} para abrirlo, {@code false} para cerrarlo
     * @return el restaurante con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public Restaurant cambiarEstado(String id, boolean abierto) throws IOException {
        Restaurant restaurante = obtenerPorId(id);
        restaurante.setOpen(abierto);
        restaurantDao.actualizar(restaurante);
        return restaurante;
    }

    /**
     * Registra un producto en el catálogo de un restaurante. El producto queda
     * disponible por defecto.
     *
     * @param idProducto    identificador del producto (debe ser único)
     * @param idRestaurante identificador del restaurante al que pertenece
     * @param nombre        nombre del producto
     * @param descripcion   descripción del producto
     * @param precio        precio unitario (mayor que cero)
     * @return el producto registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es inválido o no existe el restaurante
     * @throws IllegalStateException    si ya existe un producto con ese id
     */
    public Product registrarProducto(String idProducto, String idRestaurante, String nombre,
                                     String descripcion, double precio) throws IOException {
        idProducto = texto(idProducto, "El id del producto");
        nombre = texto(nombre, "El nombre del producto");
        descripcion = texto(descripcion, "La descripción");
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        }

        Restaurant restaurante = obtenerPorId(idRestaurante);

        for (Product p : productDao.listar()) {
            if (p.getId().equals(idProducto)) {
                throw new IllegalStateException("Ya existe un producto con el id " + idProducto + ".");
            }
        }

        Product producto = new Product(idProducto, restaurante.getId(), nombre, descripcion, precio);
        productDao.guardar(producto);
        return producto;
    }

    /**
     * Lista los productos de un restaurante.
     *
     * @param idRestaurante identificador del restaurante
     * @return lista de productos del restaurante (vacía si no tiene ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public List<Product> listarProductos(String idRestaurante) throws IOException {
        Restaurant restaurante = obtenerPorId(idRestaurante);
        List<Product> resultado = new ArrayList<>();
        for (Product p : productDao.listar()) {
            if (p.getRestaurantId().equals(restaurante.getId())) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Valida que un texto no sea vacío y no contenga el separador {@code ;}
     * usado en el archivo.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto sin espacios al inicio ni al final
     * @throws IllegalArgumentException si el texto es nulo, vacío o contiene {@code ;}
     */
    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        if (valor.contains(";")) {
            throw new IllegalArgumentException(campo + " no puede contener el carácter ';'.");
        }
        return valor.trim();
    }
}