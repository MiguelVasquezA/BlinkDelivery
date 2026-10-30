package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Order;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en disco plano para la entidad {@link Order}.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class OrderFileDao {
    private static final String RUTA = "data/orders.txt";

    /**
     * Guarda un pedido nuevo al final del archivo de texto.
     *
     * @param order Pedido a registrar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(Order order) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(order.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Genera un listado completo de pedidos guardados.
     *
     * @return Lista de instancias de {@link Order}.
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Order> listar() throws IOException {
        List<Order> orders = new ArrayList<>();
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        for (String linea : lineas) {
            String[] partes = linea.split(";");
            if (partes.length == 8) {
                Order o = new Order(partes[0], partes[1], partes[2], partes[3], Integer.parseInt(partes[4]), Double.parseDouble(partes[5]));
                o.setStatus(partes[6]);
                o.setDriverId(partes[7]);
                orders.add(o);
            }
        }
        return orders;
    }

    /**
     * Obtiene una orden buscando por su código ID único.
     *
     * @param id Identificador de la orden.
     * @return La orden encontrada o {@code null}.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Order buscarPorId(String id) throws IOException {
        for (Order o : listar()) {
            if (o.getId().equalsIgnoreCase(id)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Sobrescribe el registro de una orden existente para actualizar su estado o domiciliario asignado.
     *
     * @param order Orden con la información modificada.
     * @throws IOException Si no se puede actualizar el archivo plano.
     */
    public void actualizar(Order order) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Order o : listar()) {
            lineas.add(o.getId().equalsIgnoreCase(order.getId()) ? order.toString() : o.toString());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}