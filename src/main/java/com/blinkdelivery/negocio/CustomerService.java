package com.blinkdelivery.negocio;

import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.modelo.Customer;
import java.io.IOException;
import java.util.List;

/**
 * Servicio de negocio para la gestión de clientes.
 * <p>
 * Aplica las validaciones y reglas del dominio antes de delegar la
 * persistencia en {@link CustomerFileDao}.
 * </p>
 */
public class CustomerService {

    /** DAO que guarda y lee los clientes en el archivo de texto. */
    private final CustomerFileDao customerDao = new CustomerFileDao();

    /**
     * Registra un nuevo cliente en el sistema.
     *
     * @param identificacion número de identificación del cliente (debe ser único)
     * @param nombre         nombre completo del cliente
     * @param telefono       teléfono de contacto (entre 7 y 10 dígitos)
     * @param direccion      dirección de entrega del cliente
     * @return el cliente registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya existe un cliente con esa identificación
     */
    public Customer registrarCliente(String identificacion, String nombre,
                                     String telefono, String direccion) throws IOException {
        identificacion = texto(identificacion, "La identificación");
        nombre = texto(nombre, "El nombre");
        telefono = validarTelefono(telefono);
        direccion = texto(direccion, "La dirección");

        if (buscarPorId(identificacion) != null) {
            throw new IllegalStateException("Ya existe un cliente con la identificación " + identificacion + ".");
        }

        Customer cliente = new Customer(identificacion, nombre, telefono, direccion);
        customerDao.guardar(cliente);
        return cliente;
    }

    /**
     * Actualiza el nombre, teléfono y dirección de un cliente existente.
     *
     * @param identificacion identificación del cliente a actualizar
     * @param nombre         nuevo nombre
     * @param telefono       nuevo teléfono (entre 7 y 10 dígitos)
     * @param direccion      nueva dirección
     * @return el cliente con los datos actualizados
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el cliente no existe o algún dato es inválido
     */
    public Customer actualizarCliente(String identificacion, String nombre,
                                      String telefono, String direccion) throws IOException {
        Customer cliente = obtenerPorId(identificacion);
        cliente.setName(texto(nombre, "El nombre"));
        cliente.setPhone(validarTelefono(telefono));
        cliente.setAddress(texto(direccion, "La dirección"));
        customerDao.actualizar(cliente);
        return cliente;
    }

    /**
     * Lista todos los clientes registrados.
     *
     * @return lista de clientes (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Customer> listarClientes() throws IOException {
        return customerDao.listar();
    }

    /**
     * Busca un cliente por su identificación.
     *
     * @param identificacion identificación a buscar
     * @return el cliente encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Customer buscarPorId(String identificacion) throws IOException {
        if (identificacion == null) {
            return null;
        }
        String id = identificacion.trim();
        for (Customer c : customerDao.listar()) {
            if (c.getIdentification().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Obtiene un cliente por su identificación y falla si no existe.
     *
     * @param identificacion identificación a buscar
     * @return el cliente encontrado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe un cliente con esa identificación
     */
    public Customer obtenerPorId(String identificacion) throws IOException {
        Customer cliente = buscarPorId(identificacion);
        if (cliente == null) {
            throw new IllegalArgumentException("No existe un cliente con la identificación " + identificacion + ".");
        }
        return cliente;
    }

    /**
     * Valida que un texto no sea vacío y no contenga el separador {@code ;}
     * usado en el archivo.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto sin espacios al inicio ni al final
     * @throws IllegalArgumentException si el texto es nulo, vacío o contiene {@code ;}
     */
    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        if (valor.contains(";")) {
            throw new IllegalArgumentException(campo + " no puede contener el carácter ';'.");
        }
        return valor.trim();
    }

    /**
     * Valida que el teléfono tenga entre 7 y 10 dígitos.
     *
     * @param telefono teléfono a validar
     * @return el teléfono sin espacios al inicio ni al final
     * @throws IllegalArgumentException si el teléfono es vacío o no cumple el formato
     */
    private static String validarTelefono(String telefono) {
        String t = texto(telefono, "El teléfono");
        if (!t.matches("\\d{7,10}")) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 10 dígitos.");
        }
        return t;
    }
}