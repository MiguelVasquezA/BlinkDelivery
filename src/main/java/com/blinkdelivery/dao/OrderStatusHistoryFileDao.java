package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.OrderStatusChange;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en archivo plano del historial de estados de los pedidos (RN18).
 * <p>
 * Formato de cada línea: {@code idPedido;ESTADO;fechaHora}.
 * </p>
 *
 * @author BlinkDelivery
 * @version 1.0
 */
public class OrderStatusHistoryFileDao {
    private static final String RUTA = "data/order_status_history.txt";

    /**
     * Agrega un cambio de estado al final del historial.
     *
     * @param cambio Cambio de estado a registrar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(OrderStatusChange cambio) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(cambio.toRegistro());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Lista los cambios de estado de un pedido en orden cronológico de registro.
     *
     * @param idPedido Identificador del pedido.
     * @return Historial del pedido (vacío si no tiene registros).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<OrderStatusChange> listarPorPedido(String idPedido) throws IOException {
        List<OrderStatusChange> historial = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 3 && p[0].equalsIgnoreCase(idPedido)) {
                historial.add(new OrderStatusChange(p[0], OrderStatus.desdeTexto(p[1]), LocalDateTime.parse(p[2])));
            }
        }
        return historial;
    }
}
