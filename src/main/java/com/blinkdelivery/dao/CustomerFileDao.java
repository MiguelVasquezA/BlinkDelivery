package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Customer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en archivo de texto para la entidad {@link Customer}.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class CustomerFileDao {
    private static final String RUTA = "data/customers.txt";

    /**
     * Agrega un nuevo cliente al final del archivo de datos.
     *
     * @param customer Objeto cliente a almacenar.
     * @throws IOException Si falla la escritura en el archivo.
     */
    public void guardar(Customer customer) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(customer.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Recupera y mapea todos los clientes almacenados en el archivo plano.
     *
     * @return Lista de clientes registrados.
     * @throws IOException Si ocurre un error al leer el archivo.
     */
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

    /**
     * Busca un cliente registrado a partir de su número de identificación.
     *
     * @param identification Documento de identidad del cliente a buscar.
     * @return El objeto {@link Customer} si lo encuentra, o {@code null} en caso contrario.
     * @throws IOException Si ocurre un error de lectura de archivo.
     */
    public Customer buscarPorId(String identification) throws IOException {
        for (Customer c : listar()) {
            if (c.getIdentification().equalsIgnoreCase(identification)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Reemplaza la información de un cliente existente con sus datos actualizados.
     *
     * @param customer Instancia con la información actualizada del cliente.
     * @throws IOException Si falla la reescritura en el archivo.
     */
    public void actualizar(Customer customer) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Customer c : listar()) {
            lineas.add(c.getIdentification().equalsIgnoreCase(customer.getIdentification()) ? customer.toString() : c.toString());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}