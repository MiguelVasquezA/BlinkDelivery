package com.blinkdelivery.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexiones JDBC a PostgreSQL (Supabase).
 * <p>
 * La conexión se lee solo de variables de entorno; no hay valores por defecto ni secretos en el código:
 * </p>
 * <ul>
 *   <li>{@code DATABASE_URL}: p. ej. {@code jdbc:postgresql://host:5432/postgres?sslmode=require}</li>
 *   <li>{@code DATABASE_USER}</li>
 *   <li>{@code DATABASE_PASSWORD}</li>
 * </ul>
 */
public final class Db {
    static final String VAR_URL = "DATABASE_URL";
    static final String VAR_USUARIO = "DATABASE_USER";
    static final String VAR_CLAVE = "DATABASE_PASSWORD";

    private Db() {
    }

    /**
     * Abre una conexión nueva. Quien la use debe cerrarla (try-with-resources).
     *
     * @return conexión JDBC abierta
     * @throws SQLException             si no se puede conectar
     * @throws IllegalStateException    si falta alguna variable de entorno
     */
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(
                requerida(VAR_URL), requerida(VAR_USUARIO), requerida(VAR_CLAVE));
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre + ".");
        }
        return valor.trim();
    }

    /**
     * Convierte un fallo de PostgreSQL en una {@link java.io.IOException} que incluye el
     * SQLState y el mensaje original, y lo escribe en {@code System.err} para los logs.
     *
     * @param contexto descripción de la operación que falló
     * @param e        excepción original de JDBC
     * @return excepción lista para lanzar
     */
    static java.io.IOException error(String contexto, SQLException e) {
        String detalle = contexto + " [SQLState " + e.getSQLState() + "]: " + e.getMessage();
        System.err.println("BD ERROR: " + detalle);
        e.printStackTrace(System.err);
        return new java.io.IOException(detalle, e);
    }
}
