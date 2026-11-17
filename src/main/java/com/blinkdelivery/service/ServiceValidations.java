package com.blinkdelivery.service;

/**
 * Validaciones de entrada comunes a los servicios.
 * <p>
 * Además de las reglas de formato, impiden el carácter {@code ;} y los saltos de
 * línea porque la persistencia usa archivos de texto con un registro por línea
 * y campos separados por {@code ;}.
 * </p>
 */
final class ServiceValidations {

    private static final String TELEFONO = "\\d{7,10}";

    private ServiceValidations() {
    }

    /**
     * Valida un texto obligatorio.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto sin espacios al inicio ni al final
     * @throws IllegalArgumentException si es nulo, vacío o contiene {@code ;} o saltos de línea
     */
    static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return seguro(valor.trim(), campo);
    }

    /**
     * Valida un texto opcional.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto recortado, o cadena vacía si es nulo o vacío
     * @throws IllegalArgumentException si contiene {@code ;} o saltos de línea
     */
    static String opcional(String valor, String campo) {
        return valor == null ? "" : seguro(valor.trim(), campo);
    }

    /**
     * @param valor texto a revisar
     * @return {@code true} si es nulo o solo contiene espacios
     */
    static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Valida un teléfono obligatorio de 7 a 10 dígitos.
     *
     * @param telefono teléfono a validar
     * @return el teléfono recortado
     * @throws IllegalArgumentException si es vacío o no cumple el formato
     */
    static String telefono(String telefono) {
        String t = texto(telefono, "El teléfono");
        if (!t.matches(TELEFONO)) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 10 dígitos.");
        }
        return t;
    }

    /**
     * Valida un teléfono opcional: vacío se acepta; informado debe tener 7 a 10 dígitos.
     *
     * @param telefono teléfono a validar
     * @return el teléfono recortado, o cadena vacía
     * @throws IllegalArgumentException si viene informado y no cumple el formato
     */
    static String telefonoOpcional(String telefono) {
        return vacio(telefono) ? "" : telefono(telefono);
    }

    /**
     * Valida que un precio sea un número finito mayor que cero.
     *
     * @param precio precio a validar
     * @return el mismo precio
     * @throws IllegalArgumentException si no es mayor que cero
     */
    static double precio(double precio) {
        if (Double.isNaN(precio) || Double.isInfinite(precio) || precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        }
        return precio;
    }

    private static String seguro(String valor, String campo) {
        if (valor.contains(";")) {
            throw new IllegalArgumentException(campo + " no puede contener el carácter ';'.");
        }
        if (valor.contains("\n") || valor.contains("\r")) {
            throw new IllegalArgumentException(campo + " no puede contener saltos de línea.");
        }
        return valor;
    }
}
