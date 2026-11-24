package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.OrderStatusChange;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de estados de un pedido.
 * <p>
 * Nombre heredado: antes persistía en {@code data/order_status_history.txt}. El esquema no tiene
 * tabla de historial, así que el historial se deriva del estado actual y la fecha del pedido
 * ({@code pedido.estado} y {@code pedido.fecha_pedido}). Por eso {@link #guardar} no escribe nada:
 * no se conservan los cambios intermedios.
 * </p>
 */
public class OrderStatusHistoryFileDao {

    /** No hace nada: el historial se deriva del estado del pedido en la base de datos. */
    public void guardar(OrderStatusChange cambio) throws IOException {
        // Intencionalmente vacío (ver la descripción de la clase).
    }

    /**
     * @param idPedido id del pedido ("PED-0003")
     * @return una sola entrada con el estado actual del pedido, o vacía si no existe
     * @throws IOException si falla la base de datos
     */
    public List<OrderStatusChange> listarPorPedido(String idPedido) throws IOException {
        List<OrderStatusChange> historial = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT estado, fecha_pedido FROM pedido WHERE id_pedido = ?")) {
            ps.setInt(1, Ids.pedido(idPedido));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OffsetDateTime fecha = rs.getObject("fecha_pedido", OffsetDateTime.class);
                    historial.add(new OrderStatusChange(idPedido,
                            OrderStatus.desdeTexto(rs.getString("estado")), OrderFileDao.aFechaLocal(fecha)));
                }
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo consultar el historial del pedido.", e);
        }
        return historial;
    }
}
