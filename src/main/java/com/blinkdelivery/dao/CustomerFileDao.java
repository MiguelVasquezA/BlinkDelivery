package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Customer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomerFileDao {
    private static final String RUTA = "data/customers.txt";

    public void guardar(Customer customer) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(customer.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    public List<Customer> listar() throws IOException {
        List<Customer> customers = new ArrayList<>();
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        for (String linea : lineas) {
            String[] partes = linea.split(";");
            if (partes.length == 4) {
                customers.add(new Customer(partes[0], partes[1], partes[2], partes[3]));
            }
        }
        return customers;
    }
}
