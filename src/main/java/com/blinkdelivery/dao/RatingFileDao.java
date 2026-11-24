package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Rating;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia de calificaciones sobre PostgreSQL (tabla {@code calificacion}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/ratings.txt}. Cada pedido admite una
 * calificación al restaurante ({@code id_domiciliario IS NULL}) y otra al domiciliario
 * ({@code id_domiciliario IS NOT NULL}); los índices únicos de la migración 007 lo garantizan.
 * </p>
 *
 * @author BlinkDelivery
 * @version 3.0
 */
public class RatingFileDao {
    private static final String SELECCION = """
            SELECT id_calificacion, id_pedido, id_cliente, id_restaurante, id_domiciliario,
                   puntuacion, comentario, fecha
              FROM calificacion
            """;

    /**
     * Guarda una calificación nueva.
     *
     * @param r calificación a guardar
     * @throws IOException si falla la base de datos
     */
    public void guardar(Rating r) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO calificacion (id_calificacion, id_pedido, id_cliente, id_restaurante, "
                             + "id_domiciliario, puntuacion, comentario, fecha) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, Ids.calificacion(r.getId()));
            ps.setInt(2, Ids.pedido(r.getOrderId()));
            ps.setInt(3, Ids.entero(r.getCustomerId()));
            ps.setInt(4, Ids.entero(r.getRestaurantId()));
            if (r.esDeDomiciliario()) {
                ps.setInt(5, Ids.entero(r.getDomiciliarioId()));
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, r.getEstrellas());
            ps.setString(7, r.getComentario().isEmpty() ? null : r.getComentario());
            ps.setObject(8, OrderFileDao.aFechaBase(r.getFecha()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo guardar la calificación en la base de datos.", e);
        }
    }

    /**
     * @return Todas las calificaciones (de restaurante y de domiciliario), por id.
     * @throws IOException si falla la base de datos
     */
    public List<Rating> listar() throws IOException {
        List<Rating> ratings = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " ORDER BY id_calificacion");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ratings.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new IOException("No se pudieron leer las calificaciones de la base de datos.", e);
        }
        return ratings;
    }

    /**
     * Calificación del restaurante de un pedido (no la del domiciliario).
     *
     * @param idPedido pedido calificado
     * @return la calificación al restaurante, o {@code null} si aún no existe
     * @throws IOException si falla la base de datos
     */
    public Rating buscarPorPedido(String idPedido) throws IOException {
        return buscarPorPedido(idPedido, null);
    }

    /**
     * @param idPedido       pedido calificado
     * @param idDomiciliario domiciliario calificado, o {@code null} para la calificación del restaurante
     * @return la calificación indicada, o {@code null} si aún no existe
     * @throws IOException si falla la base de datos
     */
    public Rating buscarPorPedido(String idPedido, String idDomiciliario) throws IOException {
        String condicion = idDomiciliario == null
                ? " AND id_domiciliario IS NULL"
                : " AND id_domiciliario = ?";
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " WHERE id_pedido = ?" + condicion)) {
            ps.setInt(1, Ids.pedido(idPedido));
            if (idDomiciliario != null) {
                ps.setInt(2, Ids.entero(idDomiciliario));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new IOException("No se pudo consultar la calificación en la base de datos.", e);
        }
    }

    /**
     * @param idRestaurante restaurante calificado
     * @return calificaciones al restaurante (no incluye las de domiciliario)
     * @throws IOException si falla la base de datos
     */
    public List<Rating> listarPorRestaurante(String idRestaurante) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (!r.esDeDomiciliario() && r.getRestaurantId().equals(idRestaurante)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * @param idDomiciliario domiciliario calificado
     * @return calificaciones recibidas por el domiciliario
     * @throws IOException si falla la base de datos
     */
    public List<Rating> listarPorDomiciliario(String idDomiciliario) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (idDomiciliario.equals(r.getDomiciliarioId())) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * @param idCliente cliente que calificó
     * @return calificaciones hechas por el cliente
     * @throws IOException si falla la base de datos
     */
    public List<Rating> listarPorCliente(String idCliente) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (r.getCustomerId().equals(idCliente)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    // ------------------------------------------------------------ auxiliares

    private static Rating mapear(ResultSet rs) throws SQLException {
        int idDomiciliario = rs.getInt("id_domiciliario");
        String domiciliario = rs.wasNull() ? null : String.valueOf(idDomiciliario);
        String comentario = rs.getString("comentario");
        OffsetDateTime fecha = rs.getObject("fecha", OffsetDateTime.class);
        return new Rating(
                Ids.calificacion(rs.getInt("id_calificacion")),
                Ids.pedido(rs.getInt("id_pedido")),
                String.valueOf(rs.getInt("id_cliente")),
                String.valueOf(rs.getInt("id_restaurante")),
                rs.getInt("puntuacion"),
                comentario == null ? "" : comentario,
                OrderFileDao.aFechaLocal(fecha),
                domiciliario);
    }
}
