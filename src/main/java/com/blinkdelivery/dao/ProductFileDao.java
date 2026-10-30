package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Product;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProductFileDao {
    private static final String RUTA = "data/products.txt";

    public void guardar(Product product) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(product.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

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
}
