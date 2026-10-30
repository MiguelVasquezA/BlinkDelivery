package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Order;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrderFileDao {
    private static final String RUTA = "data/orders.txt";

    public void guardar(Order order) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(order.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

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
}
