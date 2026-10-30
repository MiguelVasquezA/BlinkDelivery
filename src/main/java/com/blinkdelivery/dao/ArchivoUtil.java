package com.blinkdelivery.dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ArchivoUtil {

    public static void guardarArchivo(String ruta, List<String> contenido) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ruta))) {
            for (String linea : contenido) {
                bw.write(linea);
                bw.newLine();
            }
        }
    }

    public static List<String> leerArchivo(String ruta) throws IOException {
        List<String> lineas = new ArrayList<>();
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            return lineas; // Retorna lista vacía si el archivo aún no existe
        }
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas.add(linea);
            }
        }
        return lineas;
    }
}
