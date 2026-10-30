package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de acceso y modificación en disco para la entidad {@link Restaurant}.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class RestaurantFileDao {
    private static final String RUTA = "data/restaurants.txt";

    /**
     * Registra un nuevo restaurante en el archivo.
     *
     * @param restaurant Instancia del restaurante a guardar.
     * @throws IOException Si falla al escribir en archivo.
     */
    public void guardar(Restaurant restaurant) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(restaurant.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Devuelve la lista de restaurantes leídos desde el archivo.
     *
     * @return Lista de restaurantes.
     * @throws IOException Si ocurre un error de lectura.
     */
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

    /**
     * Busca un restaurante mediante su ID único.
     *
     * @param id Identificador a consultar.
     * @return El restaurante correspondiente o {@code null}.
     * @throws IOException Si falla la lectura de datos.
     */
    public Restaurant buscarPorId(String id) throws IOException {
        for (Restaurant r : listar()) {
            if (r.getId().equalsIgnoreCase(id)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Actualiza la información o estado de un restaurante en el archivo plano.
     *
     * @param restaurant Restaurante con los datos actualizados.
     * @throws IOException Si no se puede reescribir el archivo.
     */
    public void actualizar(Restaurant restaurant) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Restaurant r : listar()) {
            lineas.add(r.getId().equalsIgnoreCase(restaurant.getId()) ? restaurant.toString() : r.toString());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}