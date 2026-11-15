package com.blinkdelivery.service;

import com.blinkdelivery.dao.RatingFileDao;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.Rating;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Servicio de negocio para las calificaciones de restaurantes.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>RN12 / RF13: un cliente solo califica restaurantes donde hizo un pedido
 *       que ya le fue entregado.</li>
 *   <li>Solo el cliente que hizo el pedido puede calificarlo, y cada pedido
 *       admite una única calificación.</li>
 *   <li>RN13 / RN14: un cliente puede hacer muchas calificaciones y un
 *       restaurante recibir muchas (una por pedido entregado).</li>
 *   <li>La puntuación va de {@value Rating#MIN_ESTRELLAS} a {@value Rating#MAX_ESTRELLAS}
 *       y el comentario es opcional (máximo {@value #MAX_COMENTARIO} caracteres).</li>
 * </ul>
 */
public class RatingService {

    /** Puntuación mínima permitida. */
    public static final int PUNTUACION_MINIMA = Rating.MIN_ESTRELLAS;

    /** Puntuación máxima permitida. */
    public static final int PUNTUACION_MAXIMA = Rating.MAX_ESTRELLAS;

    /** Longitud máxima del comentario. */
    static final int MAX_COMENTARIO = 200;

    private static final String PREFIJO_ID = "CAL-";

    private final RatingFileDao ratingDao = new RatingFileDao();
    private final OrderService orderService = new OrderService();
    private final RestaurantService restaurantService = new RestaurantService();

    /**
     * Registra la calificación de un pedido entregado (RN12).
     *
     * @param idPedido   pedido a calificar
     * @param idCliente  cliente que califica (debe ser quien hizo el pedido)
     * @param puntuacion valor entre 1 y 5
     * @param comentario comentario opcional (máximo 200 caracteres)
     * @return la calificación registrada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si los datos son inválidos o el pedido no existe
     * @throws IllegalStateException    si el pedido no está entregado, no es del cliente
     *                                  o ya fue calificado
     */
    public Rating registrarCalificacion(String idPedido, String idCliente, int puntuacion,
                                        String comentario) throws IOException {
        String clienteId = ServiceValidations.texto(idCliente, "El id del cliente");
        validarPuntuacion(puntuacion);
        String comentarioLimpio = comentarioOpcional(comentario);

        Order pedido = orderService.obtenerPorId(ServiceValidations.texto(idPedido, "El id del pedido"));
        if (!pedido.getCustomerId().equals(clienteId)) {
            throw new IllegalStateException("Solo el cliente que realizó el pedido puede calificarlo.");
        }
        if (pedido.getEstado() != OrderStatus.ENTREGADO) {
            throw new IllegalStateException("Solo se pueden calificar pedidos entregados (RN12). Estado actual: '"
                    + pedido.getEstado() + "'.");
        }
        if (ratingDao.buscarPorPedido(pedido.getId()) != null) {
            throw new IllegalStateException("El pedido " + pedido.getId() + " ya fue calificado.");
        }

        Rating calificacion = new Rating(siguienteId(), pedido.getId(), clienteId, pedido.getRestaurantId(),
                puntuacion, comentarioLimpio);
        ratingDao.guardar(calificacion);
        return calificacion;
    }

    /**
     * Califica un restaurante usando el pedido entregado más reciente del cliente
     * en ese restaurante que aún no haya sido calificado (RN12, RN13).
     *
     * @param idCliente     cliente que califica
     * @param idRestaurante restaurante calificado
     * @param puntuacion    valor entre 1 y 5
     * @param comentario    comentario opcional (máximo 200 caracteres)
     * @return la calificación registrada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si los datos son inválidos o no existe el restaurante
     * @throws IllegalStateException    si el cliente no tiene compras entregadas en el
     *                                  restaurante o ya las calificó todas
     */
    public Rating calificarRestaurante(String idCliente, String idRestaurante, int puntuacion,
                                       String comentario) throws IOException {
        Restaurant restaurante = restaurantService.obtenerPorId(
                ServiceValidations.texto(idRestaurante, "El id del restaurante"));
        List<Order> entregados = orderService.listarEntregados(idCliente, restaurante.getId());
        if (entregados.isEmpty()) {
            throw new IllegalStateException("El cliente solo puede calificar restaurantes donde haya realizado "
                    + "y recibido un pedido (RN12).");
        }
        Order porCalificar = null;
        for (Order o : entregados) {
            if (ratingDao.buscarPorPedido(o.getId()) == null
                    && (porCalificar == null || o.getFechaHora().isAfter(porCalificar.getFechaHora()))) {
                porCalificar = o;
            }
        }
        if (porCalificar == null) {
            throw new IllegalStateException("El cliente ya calificó todos sus pedidos entregados en '"
                    + restaurante.getNombre() + "'.");
        }
        return registrarCalificacion(porCalificar.getId(), idCliente, puntuacion, comentario);
    }

    /**
     * Indica si un cliente puede calificar un restaurante: tiene al menos un pedido
     * entregado allí que aún no ha calificado (RN12).
     *
     * @param idCliente     cliente
     * @param idRestaurante restaurante
     * @return {@code true} si puede calificar
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si algún id es vacío
     */
    public boolean puedeCalificar(String idCliente, String idRestaurante) throws IOException {
        for (Order o : orderService.listarEntregados(idCliente, idRestaurante)) {
            if (ratingDao.buscarPorPedido(o.getId()) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca la calificación de un pedido.
     *
     * @param idPedido pedido a consultar
     * @return la calificación, o {@code null} si el pedido no ha sido calificado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public Rating buscarPorPedido(String idPedido) throws IOException {
        return ratingDao.buscarPorPedido(ServiceValidations.texto(idPedido, "El id del pedido"));
    }

    /**
     * @param idPedido pedido a consultar
     * @return {@code true} si ya tiene calificación
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public boolean estaCalificado(String idPedido) throws IOException {
        return buscarPorPedido(idPedido) != null;
    }

    /**
     * Lista las calificaciones recibidas por un restaurante (RF14, RN14).
     *
     * @param idRestaurante restaurante a consultar
     * @return calificaciones del restaurante (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el restaurante no existe
     */
    public List<Rating> listarPorRestaurante(String idRestaurante) throws IOException {
        Restaurant restaurante = restaurantService.obtenerPorId(idRestaurante);
        return ratingDao.listarPorRestaurante(restaurante.getId());
    }

    /**
     * Lista las calificaciones hechas por un cliente (RN13).
     *
     * @param idCliente cliente a consultar
     * @return calificaciones del cliente (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public List<Rating> listarPorCliente(String idCliente) throws IOException {
        return ratingDao.listarPorCliente(ServiceValidations.texto(idCliente, "El id del cliente"));
    }

    /**
     * Calcula la calificación promedio de un restaurante, redondeada a dos decimales.
     *
     * @param idRestaurante restaurante a consultar
     * @return el promedio, o {@code 0} si no tiene calificaciones
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el restaurante no existe
     */
    public double calcularPromedio(String idRestaurante) throws IOException {
        List<Rating> calificaciones = listarPorRestaurante(idRestaurante);
        if (calificaciones.isEmpty()) {
            return 0.0;
        }
        long suma = 0;
        for (Rating r : calificaciones) {
            suma += r.getEstrellas();
        }
        return BigDecimal.valueOf(suma)
                .divide(BigDecimal.valueOf(calificaciones.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void validarPuntuacion(int puntuacion) {
        if (puntuacion < PUNTUACION_MINIMA || puntuacion > PUNTUACION_MAXIMA) {
            throw new IllegalArgumentException("La puntuación debe estar entre " + PUNTUACION_MINIMA
                    + " y " + PUNTUACION_MAXIMA + ".");
        }
    }

    private static String comentarioOpcional(String comentario) {
        String limpio = ServiceValidations.opcional(comentario, "El comentario");
        if (limpio.length() > MAX_COMENTARIO) {
            throw new IllegalArgumentException("El comentario no puede tener más de "
                    + MAX_COMENTARIO + " caracteres.");
        }
        return limpio;
    }

    /** Genera el siguiente id {@code CAL-NNNN} a partir del mayor existente (SP08). */
    private String siguienteId() throws IOException {
        int max = 0;
        for (Rating r : ratingDao.listar()) {
            String id = r.getId();
            if (id.startsWith(PREFIJO_ID) && id.substring(PREFIJO_ID.length()).matches("\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(PREFIJO_ID.length())));
            }
        }
        return PREFIJO_ID + String.format("%04d", max + 1);
    }
}
