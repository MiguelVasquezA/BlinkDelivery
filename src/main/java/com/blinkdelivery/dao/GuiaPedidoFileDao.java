package com.blinkdelivery.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Guía de seguimiento de cada pedido.
 * <p>
 * Nombre heredado: antes persistía en {@code data/guias_pedido.txt}. El esquema no tiene tabla
 * para la guía, así que el texto se guarda en {@code pedido.notas}. Si el pedido no tiene texto,
 * la guía se deriva del id y el estado actual, p. ej. {@code "Seguimiento PED-0003 · EN_CAMINO"}.
 * </p>
 */
public class GuiaPedidoFileDao {

    /**
     * @return guía de cada pedido, por id ("PED-0003")
     * @throws IOException si falla la base de datos
     */
    public Map<String, String> listar() throws IOException {
        Map<String, String> guias = new HashMap<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT id_pedido, estado, notas FROM pedido");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = Ids.pedido(rs.getInt("id_pedido"));
                String notas = rs.getString("notas");
                guias.put(id, notas != null && !notas.isBlank() ? notas
                        : "Seguimiento " + id + " · " + rs.getString("estado"));
            }
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer las guías de la base de datos.", e);
        }
        return guias;
    }

    /**
     * Guarda el texto de la guía del pedido (sin punto y coma ni saltos de línea).
     *
     * @param idPedido id del pedido ("PED-0003")
     * @param texto    texto de la guía
     * @throws IOException si falla la base de datos
     */
    public void guardar(String idPedido, String texto) throws IOException {
        String limpio = texto == null ? "" : texto.replace(';', ',').replaceAll("[\\r\\n]+", " ").trim();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE pedido SET notas = ? WHERE id_pedido = ?")) {
            ps.setString(1, limpio.isEmpty() ? null : limpio);
            ps.setInt(2, Ids.pedido(idPedido));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar la guía en la base de datos.", e);
        }
    }
}
