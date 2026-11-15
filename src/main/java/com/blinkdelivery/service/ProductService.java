package com.blinkdelivery.service;

import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para el catálogo de productos.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>RN05 / SP03: todo producto pertenece a un único restaurante existente;
 *       el restaurante no se puede cambiar después.</li>
 *   <li>RN06: un restaurante puede ofrecer muchos productos.</li>
 *   <li>RN16: los productos están Disponibles o No disponibles.</li>
 *   <li>SP08: el identificador del producto es único en todo el sistema.</li>
 * </ul>
 */
public class ProductService {

    /** DAO que guarda y lee los productos en el archivo de texto. */
    private final ProductFileDao productDao = new ProductFileDao();

    /** DAO usado para verificar que el restaurante exista. */
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();

    /**
     * Registra un producto en el catálogo de un restaurante. Queda disponible.
     *
     * @param idProducto    identificador del producto (único en el sistema)
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
        String id = ServiceValidations.texto(idProducto, "El id del producto");
        String nom = ServiceValidations.texto(nombre, "El nombre del producto");
        String desc = ServiceValidations.texto(descripcion, "La descripción");
        ServiceValidations.precio(precio);
        Restaurant restaurante = obtenerRestaurante(idRestaurante);

        if (buscarPorId(id) != null) {
            throw new IllegalStateException("Ya existe un producto con el id " + id + ".");
        }

        Product producto = new Product(id, restaurante.getId(), nom, desc, precio);
        productDao.guardar(producto);
        return producto;
    }

    /**
     * Actualiza nombre, descripción y precio de un producto. El restaurante no
     * cambia (RN05). Los pedidos ya creados conservan el precio con que se hicieron.
     *
     * @param idProducto  producto a actualizar
     * @param nombre      nuevo nombre
     * @param descripcion nueva descripción
     * @param precio      nuevo precio (mayor que cero)
     * @return el producto actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el producto no existe o algún dato es inválido
     */
    public Product actualizarProducto(String idProducto, String nombre, String descripcion,
                                      double precio) throws IOException {
        Product producto = obtenerPorId(idProducto);
        producto.setNombre(ServiceValidations.texto(nombre, "El nombre del producto"));
        producto.setDescripcion(ServiceValidations.texto(descripcion, "La descripción"));
        producto.setPrecio(ServiceValidations.precio(precio));
        productDao.actualizar(producto);
        return producto;
    }

    /**
     * Marca un producto como Disponible o No disponible (RN16, RF15).
     *
     * @param idProducto producto
     * @param disponible nuevo estado
     * @return el producto actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el producto no existe
     */
    public Product cambiarDisponibilidad(String idProducto, boolean disponible) throws IOException {
        Product producto = obtenerPorId(idProducto);
        producto.setDisponible(disponible);
        productDao.actualizar(producto);
        return producto;
    }

    /**
     * Lista todos los productos de un restaurante, disponibles o no.
     *
     * @param idRestaurante restaurante
     * @return productos del restaurante (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public List<Product> listarPorRestaurante(String idRestaurante) throws IOException {
        Restaurant restaurante = obtenerRestaurante(idRestaurante);
        List<Product> resultado = new ArrayList<>();
        for (Product p : productDao.listar()) {
            if (p.getRestaurantId().equals(restaurante.getId())) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Lista los productos disponibles de un restaurante, para que el cliente los
     * consulte antes de comprar (RF06).
     *
     * @param idRestaurante restaurante
     * @return productos disponibles (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el restaurante
     */
    public List<Product> listarDisponibles(String idRestaurante) throws IOException {
        List<Product> resultado = new ArrayList<>();
        for (Product p : listarPorRestaurante(idRestaurante)) {
            if (p.isDisponible()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * @return todos los productos registrados
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Product> listarProductos() throws IOException {
        return productDao.listar();
    }

    /**
     * Busca un producto por su identificador.
     *
     * @param idProducto identificador
     * @return el producto, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Product buscarPorId(String idProducto) throws IOException {
        if (ServiceValidations.vacio(idProducto)) {
            return null;
        }
        String id = idProducto.trim();
        for (Product p : productDao.listar()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Obtiene un producto y falla si no existe.
     *
     * @param idProducto identificador
     * @return el producto
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe
     */
    public Product obtenerPorId(String idProducto) throws IOException {
        Product producto = buscarPorId(idProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe un producto con el id " + idProducto + ".");
        }
        return producto;
    }

    /**
     * Obtiene un producto validando que pertenezca al restaurante indicado (RN05, RN09).
     *
     * @param idProducto    producto
     * @param idRestaurante restaurante esperado
     * @return el producto
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el producto no existe
     * @throws IllegalStateException    si pertenece a otro restaurante
     */
    public Product obtenerDeRestaurante(String idProducto, String idRestaurante) throws IOException {
        Product producto = obtenerPorId(idProducto);
        if (!producto.getRestaurantId().equals(ServiceValidations.texto(idRestaurante, "El id del restaurante"))) {
            throw new IllegalStateException("El producto '" + producto.getNombre()
                    + "' no pertenece al restaurante " + idRestaurante.trim() + ".");
        }
        return producto;
    }

    private Restaurant obtenerRestaurante(String idRestaurante) throws IOException {
        String id = ServiceValidations.texto(idRestaurante, "El id del restaurante");
        for (Restaurant r : restaurantDao.listar()) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        throw new IllegalArgumentException("No existe un restaurante con el id " + id + ".");
    }
}
