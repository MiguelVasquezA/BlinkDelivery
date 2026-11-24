package com.blinkdelivery.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Operaciones compartidas sobre la tabla {@code persona}, que es la base de
 * {@code cliente} y {@code domiciliario}.
 * <ul>
 *   <li>El nombre completo se parte en {@code nombre} (primera palabra) y {@code apellido} (el resto).</li>
 *   <li>El teléfono es obligatorio al guardar.</li>
 * </ul>
 */
final class Personas {
    static final String DOMINIO_SIN_CORREO = "@blinkdelivery.local";

    private Personas() {
    }

    /** @return {@code [nombre, apellido]}; el apellido queda vacío si solo hay una palabra. */
    static String[] partir(String nombreCompleto) {
        String limpio = nombreCompleto == null ? "" : nombreCompleto.trim().replaceAll("\\s+", " ");
        int espacio = limpio.indexOf(' ');
        if (espacio < 0) {
            return new String[] {limpio, ""};
        }
        return new String[] {limpio.substring(0, espacio), limpio.substring(espacio + 1)};
    }

    /** @return nombre y apellido unidos en un solo texto. */
    static String unir(String nombre, String apellido) {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellido == null ? "" : apellido.trim();
        return a.isEmpty() ? n : n + " " + a;
    }

    /** Valida que haya teléfono; lo exige el registro y la edición. */
    static String telefonoObligatorio(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio.");
        }
        return telefono.trim();
    }

    /** Correo para mostrar cuando la base no tiene uno: {@code <id>@blinkdelivery.local}. */
    static String correoPorDefecto(String correo, String id, String dominioAlternativo) {
        if (correo != null && !correo.isBlank()) {
            return correo.trim();
        }
        return dominioAlternativo == null ? "sin-correo-" + id + DOMINIO_SIN_CORREO
                : id + dominioAlternativo;
    }

    /** Inserta o actualiza la fila de {@code persona} con ese id. */
    static void guardar(Connection c, int id, String nombreCompleto, String telefono) throws SQLException {
        String[] partes = partir(nombreCompleto);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO persona (id_persona, nombre, apellido, telefono) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (id_persona) DO UPDATE SET nombre = EXCLUDED.nombre, "
                        + "apellido = EXCLUDED.apellido, telefono = EXCLUDED.telefono")) {
            ps.setInt(1, id);
            ps.setString(2, partes[0]);
            ps.setString(3, partes[1]);
            ps.setString(4, telefono);
            ps.executeUpdate();
        }
    }
}
