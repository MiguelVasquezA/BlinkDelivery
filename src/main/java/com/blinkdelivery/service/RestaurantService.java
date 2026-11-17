package com.blinkdelivery.service;

import com.blinkdelivery.dao.CategoryFileDao;
import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RatingFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Category;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Rating;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.modelo.RestaurantStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para restaurantes y sus categorías.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>RN03 / RN04 / SP02: todo restaurante tiene al menos una categoría y una
 *       categoría puede estar en muchos restaurantes.</li>
 *   <li>RN15: los restaurantes están Abiertos o Cerrados.</li>
 *   <li>RN19: el registro no tiene costo de inscripción (no se cobra nada al registrar).</li>
 * </ul>
 * <p>
 * Los restaurantes que devuelve este servicio vienen completos: con sus
 * categorías, sus productos y sus calificaciones (y por tanto su promedio).
 * La gestión de productos se delega en {@link ProductService}.
 * </p>
 */
public class RestaurantService {

    /** DAO que guarda y lee los restaurantes en el archivo de texto. */
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();

    /** DAO de categorías y de la relación restaurante–categoría. */
    private final CategoryFileDao categoryDao = new CategoryFileDao();

    /** DAO de productos, para armar el catálogo del restaurante. */
    private final ProductFileDao productDao = new ProductFileDao();

    /** DAO de calificaciones, para calcular el promedio del restaurante. */
    private final RatingFileDao ratingDao = new RatingFileDao();

    /** Servicio de productos al que se delega el catálogo. */
    private final ProductService productService = new ProductService();

    // ---------------------------------------------------------------- Restaurantes

    /**
     * Registra un restaurante con una categoría, que queda abierto. La categoría
     * se crea si no existe (SP02).
     *
     * @param id        identificador del restaurante (único)
     * @param nombre    nombre del restaurante
     * @param categoria nombre de la categoría de comida (por ejemplo, Comidas Rápidas)
     * @return el restaurante registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya existe un restaurante con ese id
     */
    public Restaurant registrarRestaurante(String id, String nombre, String categoria) throws IOException {
        List<String> categorias = new ArrayList<>();
        categorias.add(categoria);
        return registrarRestaurante(id, nombre, categoria, categorias);
    }

    /**
     * Registra un restaurante con una o varias categorías (RN03, SP02).
     *
     * @param id          identificador del restaurante (único)
     * @param nombre      nombre del restaurante
     * @param tipoCocina  tipo de cocina principal
     * @param categorias  nombres de las categorías (al menos una); se crean si no existen
     * @return el restaurante registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es inválido o no hay categorías
     * @throws IllegalStateException    si ya existe un restaurante con ese id
     */
    public Restaurant registrarRestaurante(String id, String nombre, String tipoCocina,
                                          List<String> categorias) throws IOException {
        String idRest = ServiceValidations.texto(id, "El id del restaurante");
        String nom = ServiceValidations.texto(nombre, "El nombre");
        String tipo = ServiceValidations.texto(tipoCocina, "El tipo de cocina");
        if (categorias == null || categorias.isEmpty()) {
            throw new IllegalArgumentException("El restaurante debe tener al menos una categoría (SP02).");
        }
        List<String> nombres = new ArrayList<>();
        for (String c : categorias) {
            nombres.add(ServiceValidations.texto(c, "La categoría"));
        }

        if (buscarRegistro(idRest) != null) {
            throw new IllegalStateException("Ya existe un restaurante con el id " + idRest + ".");
        }

        Restaurant restaurante = new Restaurant(idRest, nom, tipo);
        restaurantDao.guardar(restaurante);
        for (String n : nombres) {
            Category categoria = obtenerOCrearCategoria(n);
            categoryDao.vincular(idRest, categoria.getId());
            if (!restaurante.getCategorias().contains(categoria)) {
                restaurante.agregarCategoria(categoria);
            }
        }
        return restaurante;
    }

    /**
     * Actualiza el nombre y el tipo de cocina de un restaurante. Si el tipo de
     * cocina corresponde a una categoría que el restaurante no tiene, se le agrega.
     *
     * @param id        identificador del restaurante a actualizar
     * @param nombre    nuevo nombre
     * @param categoria nuevo tipo de cocina / categoría principal
     * @return el restaurante con los datos actualizados
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el restaurante no existe o algún dato es inválido
     */
    public Restaurant actualizarRestaurante(String id, String nombre, String categoria) throws IOException {
        Restaurant restaurante = obtenerPorId(id);
        String cat = ServiceValidations.texto(categoria, "La categoría");
        restaurante.setNombre(ServiceValidations.texto(nombre, "El nombre"));
        restaurante.setTipoCocina(cat);
        restaurantDao.actualizar(restaurante);

        Category c = obtenerOCrearCategoria(cat);
        categoryDao.vincular(restaurante.getId(), c.getId());
        return obtenerPorId(restaurante.getId());
    }

    /**
     * Lista todos los restaurantes registrados.
     *
     * @return lista de restaurantes (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Restaurant> listarRestaurantes() throws IOException {
        List<Restaurant> restaurantes = restaurantDao.listar();
        for (Restaurant r : restaurantes) {
            completar(r);
        }
        return restaurantes;
    }

    /**
     * Lista los restaurantes que están abiertos y reciben pedidos (RN15).
     *
     * @return lista de restaurantes abiertos (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Restaurant> listarAbiertos() throws IOException {
        List<Restaurant> abiertos = new ArrayList<>();
        for (Restaurant r : listarRestaurantes()) {
            if (r.estaAbierto()) {
                abiertos.add(r);
            }
        }
        return abiertos;
    }

    /**
     * Lista los restaurantes asociados a una categoría (RN04), buscándola por
     * nombre sin distinguir mayúsculas.
     *
     * @param categoria nombre de la categoría
     * @return restaurantes de esa categoría (vacía si no hay coincidencias)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si la categoría es vacía
     */
    public List<Restaurant> listarPorCategoria(String categoria) throws IOException {
        String buscada = ServiceValidations.texto(categoria, "La categoría");
        List<Restaurant> resultado = new ArrayList<>();
        for (Restaurant r : listarRestaurantes()) {
            boolean coincide = r.getTipoCocina().equalsIgnoreCase(buscada);
            for (Category c : r.getCategorias()) {
                coincide |= c.getNombre().equalsIgnoreCase(buscada);
            }
            if (coincide) {
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
        String buscado = ServiceValidations.texto(texto, "El texto de búsqueda").toLowerCase();
        List<Restaurant> resultado = new ArrayList<>();
        for (Restaurant r : listarRestaurantes()) {
            if (r.getNombre().toLowerCase().contains(buscado)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * Busca un restaurante por su identificador.
     *
     * @param id identificador a buscar
     * @return el restaurante completo, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Restaurant buscarPorId(String id) throws IOException {
        Restaurant restaurante = buscarRegistro(id);
        if (restaurante != null) {
            completar(restaurante);
        }
        return restaurante;
    }

    /**
     * Obtiene un restaurante por su identificador y falla si no existe.
     *
     * @param id identificador a buscar
     * @return el restaurante completo
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
     * Abre o cierra un restaurante (RN15).
     *
     * @param id      identificador del restaurante
     * @param abierto {@code true} para abrirlo, {@code false} para cerrarlo
     * @return el restaurante con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public Restaurant cambiarEstado(String id, boolean abierto) throws IOException {
        return cambiarEstado(id, abierto ? RestaurantStatus.ABIERTO : RestaurantStatus.CERRADO);
    }

    /**
     * Cambia el estado de un restaurante (RN15).
     *
     * @param id     identificador del restaurante
     * @param estado nuevo estado
     * @return el restaurante con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el restaurante o el estado es nulo
     */
    public Restaurant cambiarEstado(String id, RestaurantStatus estado) throws IOException {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del restaurante no puede ser nulo.");
        }
        Restaurant restaurante = obtenerPorId(id);
        restaurante.setEstado(estado);
        restaurantDao.actualizar(restaurante);
        return restaurante;
    }

    // ---------------------------------------------------------------- Categorías

    /**
     * Registra una categoría nueva.
     *
     * @param nombre      nombre de la categoría (único, sin distinguir mayúsculas)
     * @param descripcion descripción opcional
     * @return la categoría creada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el nombre es inválido
     * @throws IllegalStateException    si ya existe una categoría con ese nombre
     */
    public Category registrarCategoria(String nombre, String descripcion) throws IOException {
        String nom = ServiceValidations.texto(nombre, "El nombre de la categoría");
        String desc = ServiceValidations.opcional(descripcion, "La descripción de la categoría");
        if (categoryDao.buscarPorNombre(nom) != null) {
            throw new IllegalStateException("Ya existe la categoría '" + nom + "'.");
        }
        Category categoria = new Category(siguienteIdCategoria(), nom, desc);
        categoryDao.guardar(categoria);
        return categoria;
    }

    /**
     * @return todas las categorías registradas
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Category> listarCategorias() throws IOException {
        return categoryDao.listar();
    }

    /**
     * Asocia una categoría a un restaurante (RN03). La categoría se crea si no existe.
     *
     * @param idRestaurante   restaurante
     * @param nombreCategoria nombre de la categoría
     * @return el restaurante actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el restaurante no existe o el nombre es inválido
     * @throws IllegalStateException    si el restaurante ya tiene esa categoría
     */
    public Restaurant agregarCategoria(String idRestaurante, String nombreCategoria) throws IOException {
        Restaurant restaurante = obtenerPorId(idRestaurante);
        Category categoria = obtenerOCrearCategoria(ServiceValidations.texto(nombreCategoria, "La categoría"));
        if (restaurante.getCategorias().contains(categoria)) {
            throw new IllegalStateException("El restaurante ya tiene la categoría '" + categoria.getNombre() + "'.");
        }
        categoryDao.vincular(restaurante.getId(), categoria.getId());
        restaurante.agregarCategoria(categoria);
        return restaurante;
    }

    /**
     * Quita una categoría a un restaurante, siempre que conserve al menos una (SP02).
     *
     * @param idRestaurante restaurante
     * @param idCategoria   categoría a quitar
     * @return el restaurante actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el restaurante no existe o no tiene la categoría
     * @throws IllegalStateException    si es su única categoría
     */
    public Restaurant quitarCategoria(String idRestaurante, String idCategoria) throws IOException {
        Restaurant restaurante = obtenerPorId(idRestaurante);
        String idCat = ServiceValidations.texto(idCategoria, "El id de la categoría");
        Category categoria = null;
        for (Category c : restaurante.getCategorias()) {
            if (c.getId().equals(idCat)) {
                categoria = c;
            }
        }
        if (categoria == null) {
            throw new IllegalArgumentException("El restaurante no tiene la categoría " + idCat + ".");
        }
        if (restaurante.getCategorias().size() == 1) {
            throw new IllegalStateException("El restaurante debe conservar al menos una categoría (SP02).");
        }
        categoryDao.desvincular(restaurante.getId(), idCat);
        return obtenerPorId(restaurante.getId());
    }

    // ---------------------------------------------------------------- Productos (delegados)

    /**
     * Registra un producto en el catálogo de un restaurante. Delegado en
     * {@link ProductService#registrarProducto}.
     *
     * @param idProducto    identificador del producto (único)
     * @param idRestaurante restaurante al que pertenece
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
        return productService.registrarProducto(idProducto, idRestaurante, nombre, descripcion, precio);
    }

    /**
     * Lista los productos de un restaurante. Delegado en {@link ProductService#listarPorRestaurante}.
     *
     * @param idRestaurante identificador del restaurante
     * @return lista de productos del restaurante (vacía si no tiene ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public List<Product> listarProductos(String idRestaurante) throws IOException {
        return productService.listarPorRestaurante(idRestaurante);
    }

    // ---------------------------------------------------------------- Auxiliares

    /** Busca el registro básico del restaurante, sin completar. */
    private Restaurant buscarRegistro(String id) throws IOException {
        if (ServiceValidations.vacio(id)) {
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
     * Carga en el restaurante sus categorías, productos y calificaciones. Si un
     * restaurante antiguo no tiene categorías asociadas, se le asocia la que
     * corresponde a su tipo de cocina para cumplir SP02.
     */
    private void completar(Restaurant r) throws IOException {
        List<Category> categorias = categoryDao.listarPorRestaurante(r.getId());
        if (categorias.isEmpty()) {
            Category c = obtenerOCrearCategoria(r.getTipoCocina());
            categoryDao.vincular(r.getId(), c.getId());
            categorias.add(c);
        }
        for (Category c : categorias) {
            if (!r.getCategorias().contains(c)) {
                r.agregarCategoria(c);
            }
        }
        for (Product p : productDao.listar()) {
            if (p.getRestaurantId().equals(r.getId()) && r.buscarProducto(p.getId()) == null) {
                r.agregarProducto(p);
            }
        }
        for (Rating cal : ratingDao.listarPorRestaurante(r.getId())) {
            if (!r.getCalificaciones().contains(cal)) {
                r.agregarCalificacion(cal);
            }
        }
    }

    /** Busca una categoría por nombre o la crea si no existe. */
    private Category obtenerOCrearCategoria(String nombre) throws IOException {
        Category existente = categoryDao.buscarPorNombre(nombre);
        return existente != null ? existente : registrarCategoria(nombre, null);
    }

    /** Genera el siguiente id {@code CAT-NNN} libre. */
    private String siguienteIdCategoria() throws IOException {
        int max = 0;
        for (Category c : categoryDao.listar()) {
            if (c.getId().matches("CAT-\\d+")) {
                max = Math.max(max, Integer.parseInt(c.getId().substring(4)));
            }
        }
        return String.format("CAT-%03d", max + 1);
    }
}
