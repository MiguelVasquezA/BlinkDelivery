package com.blinkdelivery.dao;

/**
 * Clase de utilidad para operaciones de lectura y escritura sobre archivos de texto plano.
 * Asegura la creación de directorios en caso de que no existan.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Escribe una lista de cadenas en un archivo especificado en la ruta dada.
 * Crea automáticamente la carpeta contenedora si esta no existe.
 *
 * @param ruta Ubicación del archivo en disco.
 * @param contenido Lista de líneas de texto a escribir.
 * @throws IOException Si ocurre algún fallo en el acceso o escritura en disco.
 */
public class ArchivoUtil {

    public static void guardarArchivo(String ruta, List<String> contenido) throws IOException {
        File archivo = new File(ruta);
        if (archivo.getParentFile() != null) {
            archivo.getParentFile().mkdirs(); // crea la carpeta data/ si no existe
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
            for (String linea : contenido) {
                bw.write(linea);
                bw.newLine();
            }
        }
    }

    /**
     * Lee un archivo de texto plano línea por línea y retoma su contenido como lista de textos.
     *
     * @param ruta Ubicación del archivo a leer.
     * @return Lista de líneas encontradas en el archivo, o lista vacía si el archivo no existe.
     * @throws IOException Si falla el proceso de lectura física del archivo.
     */
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
