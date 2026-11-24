package com.blinkdelivery.modelo;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades comunes para leer enums desde texto (base de datos o formularios) sin fallar.
 * <p>
 * Regla: un texto desconocido nunca lanza excepción. Se avisa en {@code System.err} y se devuelve
 * el valor por defecto de cada enum.
 * </p>
 */
final class EnumTolerante {

    private EnumTolerante() {
    }

    /**
     * Clave de comparación: sin espacios sobrantes, en minúsculas, sin tildes, y con espacios o
     * guiones cambiados por guion bajo. {@code " En Camino "} → {@code "en_camino"}.
     *
     * @param texto texto leído (puede ser {@code null})
     * @return la clave normalizada; cadena vacía si el texto es {@code null}
     */
    static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "_");
    }

    /** Registra en consola un texto que no corresponde a ningún valor del enum. */
    static <E> E desconocido(String enumNombre, String texto, E porDefecto) {
        System.err.println("Aviso enum desconocido: " + enumNombre + " recibió '" + texto
                + "'; se usa " + porDefecto + ".");
        return porDefecto;
    }
}
