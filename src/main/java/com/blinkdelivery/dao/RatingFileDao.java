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
 * {@code id;idPedido;idCliente;idRestaurante;estrellas;comentario;fecha;idDomiciliario}.
 * El último campo va vacío cuando la calificación es para el restaurante. Las líneas
 * antiguas de 7 campos se leen como calificaciones de restaurante.
 * </p>
 *
 * @author BlinkDelivery
 * @version 2.0
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
                + ";" + r.getEstrellas() + ";" + r.getComentario() + ";" + r.getFecha()
                + ";" + (r.getDomiciliarioId() == null ? "" : r.getDomiciliarioId()));
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * @return Todas las calificaciones registradas (de restaurante y de domiciliario).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listar() throws IOException {
        List<Rating> ratings = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 7 || p.length == 8) {
                String domiciliario = p.length == 8 ? p[7] : null;
                ratings.add(new Rating(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]), p[5],
                        LocalDateTime.parse(p[6]), domiciliario));
            }
        }
        return ratings;
    }

    /**
     * Calificación del restaurante de un pedido (no la del domiciliario).
     *
     * @param idPedido Pedido calificado.
     * @return La calificación al restaurante, o {@code null} si aún no existe.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Rating buscarPorPedido(String idPedido) throws IOException {
        return buscarPorPedido(idPedido, null);
    }

    /**
     * @param idPedido       Pedido calificado.
     * @param idDomiciliario Domiciliario calificado, o {@code null} para la calificación del restaurante.
     * @return La calificación indicada, o {@code null} si aún no existe.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Rating buscarPorPedido(String idPedido, String idDomiciliario) throws IOException {
        for (Rating r : listar()) {
            boolean mismoTipo = idDomiciliario == null
                    ? !r.esDeDomiciliario()
                    : idDomiciliario.equalsIgnoreCase(r.getDomiciliarioId());
            if (r.getOrderId().equalsIgnoreCase(idPedido) && mismoTipo) {
                return r;
            }
        }
        return null;
    }

    /**
     * @param idRestaurante Restaurante calificado.
     * @return Calificaciones al restaurante (vacía si no tiene; no incluye las de domiciliario).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listarPorRestaurante(String idRestaurante) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (!r.esDeDomiciliario() && r.getRestaurantId().equalsIgnoreCase(idRestaurante)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * @param idDomiciliario Domiciliario calificado.
     * @return Calificaciones recibidas por el domiciliario (vacía si no tiene).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Rating> listarPorDomiciliario(String idDomiciliario) throws IOException {
        List<Rating> resultado = new ArrayList<>();
        for (Rating r : listar()) {
            if (idDomiciliario.equalsIgnoreCase(r.getDomiciliarioId())) {
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
