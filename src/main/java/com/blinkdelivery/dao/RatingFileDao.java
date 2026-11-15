package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Rating;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en archivo plano de las calificaciones (RN12–RN14).
 * <p>
 * Formato de cada línea:
 * {@code id;idPedido;idCliente;idRestaurante;estrellas;comentario;fecha}.
 * </p>
 *
 * @author BlinkDelivery
 * @version 1.0
 */
public class RatingFileDao {
    private static final String RUTA = "data/ratings.txt";

    /**
     * Guarda una calificación nueva.
     *
     * @param r Calificación a guardar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(Rating r) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(r.getId() + ";" + r.getOrderId() + ";" + r.getCustomerId() + ";" + r.getRestaurantId()
                + ";" + r.getEstrellas() + ";" + r.getComentario() + ";" + r.getFecha());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * @return Todas las calificaciones registradas.
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listar() throws IOException {
        List<Rating> ratings = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 7) {
                ratings.add(new Rating(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]), p[5],
                        LocalDateTime.parse(p[6])));
            }
        }
        return ratings;
    }

    /**
     * @param idPedido Pedido calificado.
     * @return La calificación del pedido, o {@code null} si no ha sido calificado.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Rating buscarPorPedido(String idPedido) throws IOException {
        for (Rating r : listar()) {
            if (r.getOrderId().equalsIgnoreCase(idPedido)) {
                return r;
            }
        }
        return null;
    }

    /**
     * @param idRestaurante Restaurante calificado.
     * @return Calificaciones del restaurante (vacía si no tiene).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listarPorRestaurante(String idRestaurante) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (r.getRestaurantId().equalsIgnoreCase(idRestaurante)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * @param idCliente Cliente que calificó.
     * @return Calificaciones hechas por el cliente (vacía si no tiene).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listarPorCliente(String idCliente) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (r.getCustomerId().equalsIgnoreCase(idCliente)) {
                resultado.add(r);
            }
        }
        return resultado;
    }
}
