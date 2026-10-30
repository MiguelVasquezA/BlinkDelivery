package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Product;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador de persistencia en texto para la entidad {@link Product}.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class ProductFileDao {
    private static final String RUTA = "data/products.txt";

    /**
     * Guarda un producto en la base de datos de texto plano.
     *
     * @param product Producto a ser guardado.
     * @throws IOException Si ocurre un error en la escritura.
     */
    public void guardar(Product product) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(product.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Lee y descompone los datos de productos desde el archivo.
     *
     * @return Lista de productos mapeados.
     * @throws IOException Si falla la lectura.
     */
    public List<Product> listar() throws IOException {
        List<Product> products = new ArrayList<>();
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        for (String linea : lineas) {
            String[] partes = linea.split(";");
            if (partes.length == 6) {
                Product p = new Product(partes[0], partes[1], partes[2], partes[3], Double.parseDouble(partes[4]));
                p.setAvailable(Boolean.parseBoolean(partes[5]));
                products.add(p);
            }
        }
        return products;
    }

    /**
     * Consulta un producto por su identificador.
     *
     * @param id Código del producto a buscar.
     * @return El objeto {@link Product} encontrado o {@code null}.
     * @throws IOException Si falla el proceso de lectura.
     */
    public Product buscarPorId(String id) throws IOException {
        for (Product p : listar()) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Actualiza un producto existente en el registro plano.
     *
     * @param product Datos modificados del producto.
     * @throws IOException Si no se puede actualizar el archivo.
     */
    public void actualizar(Product product) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Product p : listar()) {
            lineas.add(p.getId().equalsIgnoreCase(product.getId()) ? product.toString() : p.toString());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}