package com.blinkdelivery.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
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

    /** Pool compartido por toda la aplicación; se crea la primera vez que se necesita. */
    private static volatile HikariDataSource pool;

    private Db() {
    }

    /**
     * Toma una conexión del pool. Quien la use debe cerrarla (try-with-resources): al cerrarla
     * vuelve al pool en lugar de cerrarse.
     *
     * @return conexión JDBC del pool
     * @throws SQLException          si no hay conexión disponible en {@code connectionTimeout}
     * @throws IllegalStateException si falta alguna variable de entorno
     */
    public static Connection conectar() throws SQLException {
        return origen().getConnection();
    }

    private static HikariDataSource origen() {
        HikariDataSource actual = pool;
        if (actual == null) {
            synchronized (Db.class) {
                if (pool == null) {
                    pool = crearPool();
                }
                actual = pool;
            }
        }
        return actual;
    }

    /** Configuración pensada para el Transaction Pooler de Supabase (compatible con PgBouncer). */
    private static HikariDataSource crearPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requerida(VAR_URL));
        config.setUsername(requerida(VAR_USUARIO));
        config.setPassword(requerida(VAR_CLAVE));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(900_000);
        config.setKeepaliveTime(60_000);
        // Obligatorio con PgBouncer en modo transacción: sin sentencias preparadas con nombre.
        config.addDataSourceProperty("prepareThreshold", "0");
        config.setPoolName("blinkdelivery");
        return new HikariDataSource(config);
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
