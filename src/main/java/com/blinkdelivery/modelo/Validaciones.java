package com.blinkdelivery.modelo;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utilidades internas de validación defensiva compartidas por las clases del modelo.
 * Tiene visibilidad de paquete: no forma parte de la API pública del dominio.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
final class Validaciones {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\d{7,10}$");

    private Validaciones() {
    }

    /**
     * Valida un texto obligatorio.
     *
     * @param valor texto a validar
     * @param campo nombre del campo para el mensaje de error
     * @return el texto sin espacios al inicio ni al final
     * @throws IllegalArgumentException si es nulo o vacío
     */
    static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " es obligatorio y no puede estar vacío.");
        }
        return valor.trim();
    }

    /**
     * Normaliza un texto opcional.
     *
     * @param valor texto a normalizar
     * @return el texto recortado, o cadena vacía si es nulo
     */
    static String opcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /**
     * Valida que una referencia no sea nula.
     *
     * @param valor referencia a validar
     * @param campo nombre del campo para el mensaje de error
     * @param <T>   tipo de la referencia
     * @return la misma referencia
     * @throws IllegalArgumentException si es nula
     */
    static <T> T noNulo(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " es obligatorio y no puede ser nulo.");
        }
        return valor;
    }

    /**
     * Valida que un número sea finito y no negativo.
     *
     * @param valor número a validar
     * @param campo nombre del campo para el mensaje de error
     * @return el mismo número
     * @throws IllegalArgumentException si es negativo, NaN o infinito
     */
    static double noNegativo(double valor, String campo) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo.");
        }
        return valor;
    }

    /**
     * Valida un teléfono de 7 a 10 dígitos.
     *
     * @param valor teléfono a validar
     * @return el teléfono recortado
     * @throws IllegalArgumentException si es vacío o no cumple el formato
     */
    static String telefono(String valor) {
        String t = texto(valor, "El teléfono");
        if (!TELEFONO.matcher(t).matches()) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 10 dígitos.");
        }
        return t;
    }

    /**

     * Valida un teléfono opcional: si es nulo o vacío se guarda como cadena vacía;
     * si viene informado debe tener entre 7 y 10 dígitos (RN26).
     *
     * @param valor teléfono a validar
     * @return el teléfono recortado, o cadena vacía
     * @throws IllegalArgumentException si viene informado y no cumple el formato
     */
    static String telefonoOpcional(String valor) {
        String t = opcional(valor);
        return t.isEmpty() ? t : telefono(t);
    }

    /**
     * Valida un correo opcional: si es nulo o vacío se guarda como cadena vacía;
     * si viene informado debe tener un formato válido.
     *
     * @param valor correo a validar
     * @return el correo en minúsculas, o cadena vacía
     * @throws IllegalArgumentException si el formato es inválido
     */
    static String emailOpcional(String valor) {
        String e = opcional(valor);
        if (!e.isEmpty() && !EMAIL.matcher(e).matches()) {
            throw new IllegalArgumentException("El correo electrónico '" + e + "' no tiene un formato válido.");
        }
        return e.toLowerCase();
    }

    /**
     * Normaliza un texto para compararlo con el nombre de una constante enum:
     * quita tildes, pasa a mayúsculas y cambia espacios y guiones por guion bajo.
     *
     * @param texto texto a normalizar
     * @return clave normalizada
     */
    static String claveEnum(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().replaceAll("[\\s-]+", "_");
    }
}
