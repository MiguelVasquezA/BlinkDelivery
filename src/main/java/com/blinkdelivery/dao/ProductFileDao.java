package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Category;
import com.blinkdelivery.modelo.Product;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de productos sobre PostgreSQL (tabla {@code producto}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/products.txt}. La categoría del producto
 * es opcional y viene de {@code producto.id_categoria} (migración 008).
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class ProductFileDao {
    private static final String SELECCION = """
            SELECT p.id_producto, p.id_restaurante, p.nombre, p.descripcion, p.precio, p.disponible,
                   c.id_categoria, c.nombre AS categoria_nombre
              FROM producto p
              LEFT JOIN categoria c ON c.id_categoria = p.id_categoria
            """;

    /**
     * Registra un producto nuevo con el id que ya trae el objeto.
     *
     * @param product Producto a guardar.
     * @throws IOException Si falla la base de datos.
     */
    public void guardar(Product product) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO producto (id_producto, id_restaurante, nombre, descripcion, precio, "
                             + "disponible, id_categoria) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, aEntero(product.getId()));
            ps.setInt(2, aEntero(product.getRestaurantId()));
            ps.setString(3, product.getNombre());
            ps.setString(4, product.getDescripcion());
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(product.getPrecio()));
            ps.setBoolean(6, product.isDisponible());
            setCategoria(ps, 7, product.getCategoria());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar el producto en la base de datos.", e);
        }
    }

    /**
     * @return Todos los productos, ordenados por id.
     * @throws IOException Si falla la base de datos.
     */
    public List<Product> listar() throws IOException {
        return consultar(SELECCION + " ORDER BY p.id_producto", null);
    }

    /**
     * @param id Código del producto.
     * @return El producto, o {@code null} si no existe o el id no es numérico.
     * @throws IOException Si falla la base de datos.
     */
    public Product buscarPorId(String id) throws IOException {
        if (!esEntero(id)) {
            return null;
        }
        List<Product> r = consultar(SELECCION + " WHERE p.id_producto = ?",
                ps -> ps.setInt(1, Integer.parseInt(id.trim())));
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Actualiza nombre, descripción, precio, disponibilidad y categoría de un producto.
     *
     * @param product Datos modificados del producto.
     * @throws IOException Si falla la base de datos.
     */
    public void actualizar(Product product) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE producto SET nombre = ?, descripcion = ?, precio = ?, disponible = ?, "
                             + "id_categoria = ? WHERE id_producto = ?")) {
            ps.setString(1, product.getNombre());
            ps.setString(2, product.getDescripcion());
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(product.getPrecio()));
            ps.setBoolean(4, product.isDisponible());
            setCategoria(ps, 5, product.getCategoria());
            ps.setInt(6, aEntero(product.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Db.error("No se pudo actualizar el producto en la base de datos.", e);
        }
    }

    // ------------------------------------------------------------ auxiliares

    /** Parámetros opcionales de una consulta. */
    @FunctionalInterface
    private interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    private List<Product> consultar(String sql, Parametros parametros) throws IOException {
        List<Product> resultado = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (parametros != null) {
                parametros.asignar(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { resultado.add(mapear(rs)); } catch (SQLException | RuntimeException e) { Db.omitirFila(e); }
                }
            }
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer los productos de la base de datos.", e);
        }
        return resultado;
    }

    private static Product mapear(ResultSet rs) throws SQLException {
        Category categoria = null;
        int idCategoria = rs.getInt("id_categoria");
        if (!rs.wasNull()) {
            categoria = new Category(String.valueOf(idCategoria), rs.getString("categoria_nombre"),
                    CategoryFileDao.SIN_DESCRIPCION);
        }
        String descripcion = rs.getString("descripcion");
        Product p = new Product(String.valueOf(rs.getInt("id_producto")), rs.getString("nombre"),
                descripcion == null ? "" : descripcion, rs.getDouble("precio"), categoria,
                String.valueOf(rs.getInt("id_restaurante")));
        p.setAvailable(rs.getBoolean("disponible"));
        return p;
    }

    private static void setCategoria(PreparedStatement ps, int indice, Category categoria) throws SQLException {
        if (categoria == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, aEntero(categoria.getId()));
        }
    }

    private static boolean esEntero(String valor) {
        return valor != null && valor.trim().matches("\\d{1,9}");
    }

    private static int aEntero(String valor) {
        if (!esEntero(valor)) {
            throw new IllegalArgumentException("El id debe ser numérico: '" + valor + "'.");
        }
        return Integer.parseInt(valor.trim());
    }
}
