package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Category;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en archivo plano de las categorías y de su relación muchos a muchos
 * con los restaurantes (RN03, RN04).
 * <p>
 * {@code categories.txt}: {@code id;nombre;descripcion}<br>
 * {@code restaurant_categories.txt}: {@code idRestaurante;idCategoria}
 * </p>
 *
 * @author BlinkDelivery
 * @version 1.0
 */
public class CategoryFileDao {
    private static final String RUTA = "data/categories.txt";
    private static final String RUTA_RELACION = "data/restaurant_categories.txt";

    /**
     * Guarda una categoría nueva.
     *
     * @param categoria Categoría a guardar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(Category categoria) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(categoria.getId() + ";" + categoria.getNombre() + ";" + categoria.getDescripcion());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * @return Todas las categorías registradas.
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Category> listar() throws IOException {
        List<Category> categorias = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == 3) {
                categorias.add(new Category(p[0], p[1], p[2]));
            }
        }
        return categorias;
    }

    /**
     * @param id Identificador de la categoría.
     * @return La categoría, o {@code null} si no existe.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Category buscarPorId(String id) throws IOException {
        for (Category c : listar()) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param nombre Nombre de la categoría (sin distinguir mayúsculas).
     * @return La categoría, o {@code null} si no existe.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Category buscarPorNombre(String nombre) throws IOException {
        for (Category c : listar()) {
            if (c.getNombre().equalsIgnoreCase(nombre.trim())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Asocia una categoría a un restaurante, si no estaba asociada.
     *
     * @param idRestaurante Restaurante.
     * @param idCategoria   Categoría.
     * @throws IOException Si falla la lectura o escritura del archivo.
     */
    public void vincular(String idRestaurante, String idCategoria) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA_RELACION);
        String registro = idRestaurante + ";" + idCategoria;
        for (String linea : lineas) {
            if (linea.equalsIgnoreCase(registro)) {
                return;
            }
        }
        lineas.add(registro);
        ArchivoUtil.guardarArchivo(RUTA_RELACION, lineas);
    }

    /**
     * Quita la asociación entre una categoría y un restaurante.
     *
     * @param idRestaurante Restaurante.
     * @param idCategoria   Categoría.
     * @return {@code true} si existía la asociación.
     * @throws IOException Si falla la lectura o escritura del archivo.
     */
    public boolean desvincular(String idRestaurante, String idCategoria) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA_RELACION);
        boolean quitada = lineas.removeIf(l -> l.equalsIgnoreCase(idRestaurante + ";" + idCategoria));
        ArchivoUtil.guardarArchivo(RUTA_RELACION, lineas);
        return quitada;
    }

    /**
     * Lista las categorías asociadas a un restaurante.
     *
     * @param idRestaurante Restaurante a consultar.
     * @return Categorías del restaurante (vacía si no tiene).
     * @throws IOException Si falla la lectura de los archivos.
     */
    public List<Category> listarPorRestaurante(String idRestaurante) throws IOException {
        List<Category> resultado = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA_RELACION)) {
            String[] p = linea.split(";", -1);
            if (p.length == 2 && p[0].equalsIgnoreCase(idRestaurante)) {
                Category c = buscarPorId(p[1]);
                if (c != null && !resultado.contains(c)) {
                    resultado.add(c);
                }
            }
        }
        return resultado;
    }
}
