package com.blinkdelivery.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Credenciales de acceso ({@code sal} y {@code hash} PBKDF2).
 * <ul>
 *   <li><b>Clientes:</b> en {@code cliente.contrasena_hash} con el formato {@code sal;hash}.
 *       Un hash vacío significa que el cliente aún no tiene contraseña.</li>
 *   <li><b>Restaurantes y domiciliarios:</b> siguen en {@code data/credenciales.txt}
 *       (formato {@code rol;usuario;sal;hash}), porque el esquema no tiene dónde guardarlas.</li>
 * </ul>
 */
public class CredencialFileDao {
    private static final String RUTA = "data/credenciales.txt";
    private static final String ROL_CLIENTE = "cliente";

    /**
     * @return {sal, hash} de la cuenta, o {@code null} si el perfil aún no tiene contraseña.
     */
    public String[] buscar(String rol, String usuario) throws IOException {
        if (ROL_CLIENTE.equals(rol)) {
            return buscarCliente(usuario);
        }
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 4 && p[0].equals(rol) && p[1].equalsIgnoreCase(usuario)) {
                return new String[] {p[2], p[3]};
            }
        }
        return null;
    }

    public void guardar(String rol, String usuario, String sal, String hash) throws IOException {
        if (ROL_CLIENTE.equals(rol)) {
            guardarCliente(usuario, sal, hash);
            return;
        }
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(rol + ";" + usuario + ";" + sal + ";" + hash);
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Establece la credencial de un perfil, reemplazando la que tuviera antes (si la había).
     * A diferencia de {@link #guardar}, no duplica la línea: sirve para sembrar cuentas repetibles.
     */
    public void sustituir(String rol, String usuario, String sal, String hash) throws IOException {
        if (ROL_CLIENTE.equals(rol)) {
            guardarCliente(usuario, sal, hash);
            return;
        }
        List<String> lineas = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            boolean mismoPerfil = p.length == 4 && p[0].equals(rol) && p[1].equalsIgnoreCase(usuario);
            if (!mismoPerfil) lineas.add(linea);
        }
        lineas.add(rol + ";" + usuario + ";" + sal + ";" + hash);
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    // ------------------------------------------------------------ clientes (base de datos)

    private static String[] buscarCliente(String usuario) throws IOException {
        if (!Ids.esEntero(usuario)) {
            return null;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT contrasena_hash FROM cliente WHERE id_cliente = ?")) {
            ps.setInt(1, Ids.entero(usuario));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String guardado = rs.getString(1);
                if (guardado == null || guardado.isBlank()) {
                    return null;
                }
                String[] partes = guardado.split(";", 2);
                return partes.length == 2 ? partes : null;
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo consultar la contraseña del cliente.", e);
        }
    }

    private static void guardarCliente(String usuario, String sal, String hash) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE cliente SET contrasena_hash = ? WHERE id_cliente = ?")) {
            ps.setString(1, sal + ";" + hash);
            ps.setInt(2, Ids.entero(usuario));
            if (ps.executeUpdate() == 0) {
                throw new IOException("No existe el cliente " + usuario + " en la base de datos.");
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar la contraseña del cliente.", e);
        }
    }
}
