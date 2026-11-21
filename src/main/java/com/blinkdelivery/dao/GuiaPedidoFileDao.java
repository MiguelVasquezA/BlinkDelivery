package com.blinkdelivery.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistencia de la guía de seguimiento de cada pedido: el texto que el domiciliario
 * actualiza durante la entrega (por ejemplo, el número de guía o una nota de ubicación).
 * <p>
 * Va en un archivo aparte para no cambiar el formato de {@code orders.txt}.
 * Formato de cada línea: {@code idPedido;texto}. El texto no puede contener ';' ni saltos
 * de línea, así que se reemplazan por ',' y espacios al guardar.
 * </p>
 */
public class GuiaPedidoFileDao {
    private static final String RUTA = "data/guias_pedido.txt";

    /**
     * Devuelve la guía de todos los pedidos que la tienen.
     *
     * @return mapa id de pedido → texto de la guía
     * @throws IOException si falla la lectura del archivo
     */
    public Map<String, String> listar() throws IOException {
        Map<String, String> guias = new HashMap<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            int separador = linea.indexOf(';');
            if (separador > 0) {
                guias.put(linea.substring(0, separador), linea.substring(separador + 1));
            }
        }
        return guias;
    }

    /**
     * Establece la guía de un pedido, reemplazando la anterior si la había.
     *
     * @param idPedido identificador del pedido
     * @param texto    nuevo texto de la guía (puede ser vacío para borrarla)
     * @throws IOException si falla la escritura en disco
     */
    public void guardar(String idPedido, String texto) throws IOException {
        String limpio = texto == null ? "" : texto.replace(';', ',').replaceAll("[\\r\\n]+", " ").trim();
        List<String> lineas = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            if (!linea.startsWith(idPedido + ";")) {
                lineas.add(linea);
            }
        }
        lineas.add(idPedido + ";" + limpio);
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}
