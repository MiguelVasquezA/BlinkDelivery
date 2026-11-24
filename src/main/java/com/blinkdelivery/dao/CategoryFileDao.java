package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Category;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de categorías y de su vínculo con restaurantes, sobre PostgreSQL
 * (tablas {@code categoria} y {@code restaurante_categoria}).
 * <p>
 * La tabla {@code categoria} no tiene descripción: al leer se usa un texto por defecto.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class CategoryFileDao {
    /** Descripción que se muestra cuando la base de datos no guarda una. */
    static final String SIN_DESCRIPCION = "Sin descripción";

    /**
     * Registra una categoría nueva con el id que ya trae el objeto.
     *
     * @param categoria Categoría a guardar.
     * @throws IOException Si falla la base de datos.
     */
    public void guardar(Category categoria) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO categoria (id_categoria, nombre) VALUES (?, ?)")) {
            ps.setInt(1, aEntero(categoria.getId()));
            ps.setString(2, categoria.getNombre());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo guardar la categoría en la base de datos.", e);
        }
    }

    /**
     * @return Todas las categorías, ordenadas por id.
     * @throws IOException Si falla la base de datos.
     */
    public List<Category> listar() throws IOException {
        return consultar("SELECT id_categoria, nombre FROM categoria ORDER BY id_categoria", null);
    }

    /**
     * @param id Identificador de la categoría.
     * @return La categoría, o {@code null} si no existe o el id no es numérico.
     * @throws IOException Si falla la base de datos.
     */
    public Category buscarPorId(String id) throws IOException {
        if (!esEntero(id)) {
            return null;
        }
        List<Category> r = consultar("SELECT id_categoria, nombre FROM categoria WHERE id_categoria = ?",
                ps -> ps.setInt(1, Integer.parseInt(id.trim())));
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * @param nombre Nombre de la categoría (sin distinguir mayúsculas).
     * @return La categoría, o {@code null} si no existe.
     * @throws IOException Si falla la base de datos.
     */
    public Category buscarPorNombre(String nombre) throws IOException {
        List<Category> r = consultar("SELECT id_categoria, nombre FROM categoria WHERE lower(nombre) = lower(?)",
                ps -> ps.setString(1, nombre.trim()));
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Vincula una categoría a un restaurante (no duplica el vínculo).
     *
     * @param idRestaurante id del restaurante
     * @param idCategoria   id de la categoría
     * @throws IOException Si falla la base de datos.
     */
    public void vincular(String idRestaurante, String idCategoria) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO restaurante_categoria (id_restaurante, id_categoria) VALUES (?, ?) "
                             + "ON CONFLICT DO NOTHING")) {
            ps.setInt(1, aEntero(idRestaurante));
            ps.setInt(2, aEntero(idCategoria));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo vincular la categoría en la base de datos.", e);
        }
    }

    /**
     * Quita el vínculo entre un restaurante y una categoría.
     *
     * @return {@code true} si existía el vínculo y se quitó
     * @throws IOException Si falla la base de datos.
     */
    public boolean desvincular(String idRestaurante, String idCategoria) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM restaurante_categoria WHERE id_restaurante = ? AND id_categoria = ?")) {
            ps.setInt(1, aEntero(idRestaurante));
            ps.setInt(2, aEntero(idCategoria));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException("No se pudo desvincular la categoría en la base de datos.", e);
        }
    }

    /**
     * @param idRestaurante restaurante a consultar
     * @return categorías vinculadas al restaurante (vacía si no tiene)
     * @throws IOException Si falla la base de datos.
     */
    public List<Category> listarPorRestaurante(String idRestaurante) throws IOException {
        if (!esEntero(idRestaurante)) {
            return new ArrayList<>();
        }
        return consultar(
                "SELECT c.id_categoria, c.nombre FROM restaurante_categoria rc "
                        + "JOIN categoria c ON c.id_categoria = rc.id_categoria "
                        + "WHERE rc.id_restaurante = ? ORDER BY c.id_categoria",
                ps -> ps.setInt(1, Integer.parseInt(idRestaurante.trim())));
    }

    // ------------------------------------------------------------ auxiliares

    /** Parámetros opcionales de una consulta. */
    @FunctionalInterface
    private interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    private List<Category> consultar(String sql, Parametros parametros) throws IOException {
        List<Category> resultado = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (parametros != null) {
                parametros.asignar(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Category(String.valueOf(rs.getInt("id_categoria")),
                            rs.getString("nombre"), SIN_DESCRIPCION));
                }
            }
        } catch (SQLException e) {
            throw new IOException("No se pudieron leer las categorías de la base de datos.", e);
        }
        return resultado;
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
