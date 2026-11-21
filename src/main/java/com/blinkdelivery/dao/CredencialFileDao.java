package com.blinkdelivery.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda las credenciales de acceso en {@code data/credenciales.txt}, separadas de los
 * datos de clientes, restaurantes y domiciliarios para no alterar sus formatos.
 * Formato por línea: {@code rol;usuario;sal;hash}.
 */
public class CredencialFileDao {
    private static final String RUTA = "data/credenciales.txt";

    /**
     * @return {sal, hash} de la cuenta, o {@code null} si el perfil aún no tiene contraseña.
     */
    public String[] buscar(String rol, String usuario) throws IOException {
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 4 && p[0].equals(rol) && p[1].equalsIgnoreCase(usuario)) {
                return new String[] {p[2], p[3]};
            }
        }
        return null;
    }

    public void guardar(String rol, String usuario, String sal, String hash) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(rol + ";" + usuario + ";" + sal + ";" + hash);
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Establece la credencial de un perfil, reemplazando la que tuviera antes (si la había).
     * A diferencia de {@link #guardar}, no duplica la línea: sirve para sembrar cuentas repetibles.
     */
    public void sustituir(String rol, String usuario, String sal, String hash) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            boolean mismoPerfil = p.length == 4 && p[0].equals(rol) && p[1].equalsIgnoreCase(usuario);
            if (!mismoPerfil) lineas.add(linea);
        }
        lineas.add(rol + ";" + usuario + ";" + sal + ";" + hash);
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}
