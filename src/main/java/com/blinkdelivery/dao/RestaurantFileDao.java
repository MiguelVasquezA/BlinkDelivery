package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantFileDao {
    private static final String RUTA = "data/restaurants.txt";

    public void guardar(Restaurant restaurant) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(restaurant.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    public List<Restaurant> listar() throws IOException {
        List<Restaurant> restaurants = new ArrayList<>();
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        for (String linea : lineas) {
            String[] partes = linea.split(";");
            if (partes.length == 4) {
                Restaurant r = new Restaurant(partes[0], partes[1], partes[2]);
                r.setOpen(Boolean.parseBoolean(partes[3]));
                restaurants.add(r);
            }
        }
        return restaurants;
    }
}