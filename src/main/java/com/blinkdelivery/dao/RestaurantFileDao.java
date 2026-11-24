package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.modelo.RestaurantStatus;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de restaurantes sobre PostgreSQL (tabla {@code restaurante}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/restaurants.txt}. Ahora usa JDBC y
 * requiere {@code DATABASE_URL}, {@code DATABASE_USER} y {@code DATABASE_PASSWORD}.
 * </p>
 * <ul>
 *   <li>{@code tipoCocina} es la primera categoría vinculada en {@code restaurante_categoria}.</li>
 *   <li>La ciudad se guarda siempre como {@value #CIUDAD}.</li>
 *   <li>Los horarios van en {@code hora_apertura} y {@code hora_cierre} (migración 007).</li>
 * </ul>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class RestaurantFileDao {
    /** Ciudad por defecto de los restaurantes. */
    static final String CIUDAD = "Valledupar";

    private static final String SELECCION = """
            SELECT r.id_restaurante, r.nombre, r.telefono, r.direccion_texto, r.estado,
                   r.hora_apertura, r.hora_cierre,
                   (SELECT c.nombre
                      FROM restaurante_categoria rc
                      JOIN categoria c ON c.id_categoria = rc.id_categoria
                     WHERE rc.id_restaurante = r.id_restaurante
                     ORDER BY c.id_categoria
                     LIMIT 1) AS tipo
              FROM restaurante r
            """;

    /**
     * Registra un nuevo restaurante y vincula su tipo de cocina.
     *
     * @param restaurant Restaurante a guardar.
     * @throws IOException Si falla la base de datos.
     */
    public void guardar(Restaurant restaurant) throws IOException {
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO restaurante (id_restaurante, nombre, telefono, direccion_texto, ciudad, "
                            + "estado, hora_apertura, hora_cierre, fecha_registro) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE) RETURNING id_restaurante")) {
                ps.setInt(1, aEntero(restaurant.getId()));
                ps.setString(2, restaurant.getNombre());
                ps.setString(3, restaurant.getTelefono());
                ps.setString(4, restaurant.getDireccion());
                ps.setString(5, CIUDAD);
                ps.setString(6, estadoTexto(restaurant.isOpen()));
                ps.setObject(7, restaurant.getHoraApertura(), Types.TIME);
                ps.setObject(8, restaurant.getHoraCierre(), Types.TIME);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                }
            }
            vincularTipo(c, restaurant.getId(), restaurant.getTipoCocina());
            c.commit();
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar el restaurante en la base de datos.", e);
        }
    }

    /**
     * @return Todos los restaurantes registrados.
     * @throws IOException Si falla la base de datos.
     */
    public List<Restaurant> listar() throws IOException {
        List<Restaurant> restaurants = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " ORDER BY r.id_restaurante");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                restaurants.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer los restaurantes de la base de datos.", e);
        }
        return restaurants;
    }

    /**
     * @param id Identificador del restaurante (número, p. ej. "111").
     * @return El restaurante, o {@code null} si no existe o el id no es numérico.
     * @throws IOException Si falla la base de datos.
     */
    public Restaurant buscarPorId(String id) throws IOException {
        if (!esEntero(id)) {
            return null;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " WHERE r.id_restaurante = ?")) {
            ps.setInt(1, Integer.parseInt(id.trim()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo consultar el restaurante en la base de datos.", e);
        }
    }

    /**
     * Actualiza nombre, teléfono, dirección, estado, horario y tipo de cocina.
     *
     * @param restaurant Restaurante con los datos nuevos.
     * @throws IOException Si falla la base de datos.
     */
    public void actualizar(Restaurant restaurant) throws IOException {
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE restaurante SET nombre = ?, telefono = ?, direccion_texto = ?, estado = ?, "
                            + "hora_apertura = ?, hora_cierre = ? WHERE id_restaurante = ?")) {
                ps.setString(1, restaurant.getNombre());
                ps.setString(2, restaurant.getTelefono());
                ps.setString(3, restaurant.getDireccion());
                ps.setString(4, estadoTexto(restaurant.isOpen()));
                ps.setObject(5, restaurant.getHoraApertura(), Types.TIME);
                ps.setObject(6, restaurant.getHoraCierre(), Types.TIME);
                ps.setInt(7, aEntero(restaurant.getId()));
                ps.executeUpdate();
            }
            reemplazarTipo(c, restaurant.getId(), restaurant.getTipoCocina());
            c.commit();
        } catch (SQLException e) {
            throw Db.error("No se pudo actualizar el restaurante en la base de datos.", e);
        }
    }

    // ------------------------------------------------------------ auxiliares

    private static Restaurant mapear(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo");
        String telefono = rs.getString("telefono");
        LocalTime apertura = rs.getObject("hora_apertura", LocalTime.class);
        LocalTime cierre = rs.getObject("hora_cierre", LocalTime.class);
        Restaurant r = new Restaurant(
                String.valueOf(rs.getInt("id_restaurante")),
                rs.getString("nombre"),
                tipo == null ? Restaurant.POR_DEFINIR : tipo,
                rs.getString("direccion_texto"),
                telefono == null || telefono.isBlank() ? Restaurant.POR_DEFINIR : telefono,
                apertura == null ? Restaurant.APERTURA_POR_DEFECTO : apertura,
                cierre == null ? Restaurant.CIERRE_POR_DEFECTO : cierre);
        r.setOpen("abierto".equalsIgnoreCase(rs.getString("estado")));
        return r;
    }

    /** Deja un solo tipo de cocina: quita el actual (la primera categoría) y pone el nuevo. */
    private static void reemplazarTipo(Connection c, String idRestaurante, String tipo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM restaurante_categoria WHERE id_restaurante = ? AND id_categoria = ("
                        + "SELECT rc.id_categoria FROM restaurante_categoria rc "
                        + "JOIN categoria cat ON cat.id_categoria = rc.id_categoria "
                        + "WHERE rc.id_restaurante = ? ORDER BY cat.id_categoria LIMIT 1)")) {
            ps.setInt(1, aEntero(idRestaurante));
            ps.setInt(2, aEntero(idRestaurante));
            ps.executeUpdate();
        }
        vincularTipo(c, idRestaurante, tipo);
    }

    /** Busca o crea la categoría con ese nombre y la vincula al restaurante. */
    private static void vincularTipo(Connection c, String idRestaurante, String tipo) throws SQLException {
        if (tipo == null || tipo.isBlank() || tipo.equals(Restaurant.POR_DEFINIR)) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO categoria (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING")) {
            ps.setString(1, tipo.trim());
            ps.executeUpdate();
        }
        int idCategoria;
        try (PreparedStatement sel = c.prepareStatement("SELECT id_categoria FROM categoria WHERE nombre = ?")) {
            sel.setString(1, tipo.trim());
            try (ResultSet rs = sel.executeQuery()) {
                rs.next();
                idCategoria = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO restaurante_categoria (id_restaurante, id_categoria) VALUES (?, ?) "
                        + "ON CONFLICT DO NOTHING")) {
            ps.setInt(1, aEntero(idRestaurante));
            ps.setInt(2, idCategoria);
            ps.executeUpdate();
        }
    }

    private static String estadoTexto(boolean abierto) {
        return abierto ? RestaurantStatus.ABIERTO.name().toLowerCase() : RestaurantStatus.CERRADO.name().toLowerCase();
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
