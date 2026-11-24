package com.blinkdelivery.dao;

/**
 * Conversión entre los ids de la aplicación (texto) y las llaves enteras de PostgreSQL.
 * <ul>
 *   <li>Clientes, restaurantes, domiciliarios, productos, categorías y direcciones: {@code "111"} ↔ {@code 111}.</li>
 *   <li>Pedidos: {@code "PED-0003"} ↔ {@code 3}.</li>
 *   <li>Calificaciones: {@code "CAL-0001"} ↔ {@code 1}.</li>
 * </ul>
 */
final class Ids {
    static final String PREFIJO_PEDIDO = "PED-";
    static final String PREFIJO_CALIFICACION = "CAL-";

    private Ids() {
    }

    /** @return {@code true} si el texto es un entero positivo de hasta 9 dígitos. */
    static boolean esEntero(String valor) {
        return valor != null && valor.trim().matches("\\d{1,9}");
    }

    /** Convierte un id numérico a entero; lanza error si no lo es. */
    static int entero(String valor) {
        if (!esEntero(valor)) {
            throw new IllegalArgumentException("El id debe ser numérico: '" + valor + "'.");
        }
        return Integer.parseInt(valor.trim());
    }

    /** Convierte "PED-0003" (o "3") a entero. */
    static int pedido(String id) {
        return entero(quitarPrefijo(id, PREFIJO_PEDIDO));
    }

    /** Convierte 3 a "PED-0003". */
    static String pedido(int id) {
        return String.format("%s%04d", PREFIJO_PEDIDO, id);
    }

    /** Convierte "CAL-0001" (o "1") a entero. */
    static int calificacion(String id) {
        return entero(quitarPrefijo(id, PREFIJO_CALIFICACION));
    }

    /** Convierte 1 a "CAL-0001". */
    static String calificacion(int id) {
        return String.format("%s%04d", PREFIJO_CALIFICACION, id);
    }

    /** Devuelve el id como texto sin ceros a la izquierda (5 → "5"). */
    static String texto(int id) {
        return String.valueOf(id);
    }

    private static String quitarPrefijo(String id, String prefijo) {
        if (id == null) {
            return null;
        }
        String limpio = id.trim();
        return limpio.regionMatches(true, 0, prefijo, 0, prefijo.length())
                ? limpio.substring(prefijo.length()) : limpio;
    }
}
