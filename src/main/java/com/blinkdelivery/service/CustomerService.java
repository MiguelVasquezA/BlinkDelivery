package com.blinkdelivery.service;

import com.blinkdelivery.dao.AddresFileDao;
import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import java.io.IOException;
import java.util.List;

/**
 * Servicio de negocio para la gestión de clientes y sus direcciones.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>RN25 / SP11: el cliente es un rol de la persona; si la persona ya es
 *       domiciliario se reutilizan sus datos personales con el mismo documento.</li>
 *   <li>RN26: el teléfono es opcional para quien solo es cliente.</li>
 *   <li>RN01 / RN02: un cliente puede tener varias direcciones y cada dirección
 *       pertenece a un único cliente.</li>
 *   <li>RN27: las consultas de dirección validan que pertenezca al cliente.</li>
 * </ul>
 */
public class CustomerService {

    /** DAO que guarda y lee los clientes (con su dirección principal). */
    private final CustomerFileDao customerDao = new CustomerFileDao();

    /** DAO de las direcciones adicionales de los clientes. */
    private final AddresFileDao addressDao = new AddresFileDao();

    /** DAO de domiciliarios, para compartir los datos de la persona entre roles. */
    private final DriverFileDao driverDao = new DriverFileDao();

    /** Reglas compartidas de la generalización Persona. */
    private final PersonRoles personas = new PersonRoles(customerDao, driverDao);

    /**
     * Registra el rol de cliente de una persona.
     * <p>
     * Si la persona ya es domiciliario (mismo documento), el nombre y el teléfono
     * pueden dejarse vacíos y se reutilizan los registrados; si se informan deben
     * coincidir (SP11). El teléfono es opcional (RN26).
     * </p>
     *
     * @param identificacion documento de la persona (único)
     * @param nombre         nombre completo (opcional solo si la persona ya existe)
     * @param telefono       teléfono de 7 a 10 dígitos (opcional)
     * @param direccion      dirección principal de entrega
     * @return el cliente registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya es cliente o sus datos personales no coinciden
     */
    public Customer registrarCliente(String identificacion, String nombre,
                                     String telefono, String direccion) throws IOException {
        String id = ServiceValidations.texto(identificacion, "La identificación");
        String dir = ServiceValidations.texto(direccion, "La dirección");

        if (personas.cliente(id) != null) {
            throw new IllegalStateException("Ya existe un cliente con la identificación " + id + ".");
        }

        DeliveryDriver comoDomiciliario = personas.domiciliario(id);
        String nom;
        String tel;
        if (comoDomiciliario != null) {
            nom = PersonRoles.reutilizar(nombre, comoDomiciliario.getNombre(), "nombre", id);
            tel = PersonRoles.reutilizar(telefono, comoDomiciliario.getTelefono(), "teléfono", id);
        } else {
            nom = ServiceValidations.texto(nombre, "El nombre");
            tel = ServiceValidations.telefonoOpcional(telefono);
        }
        ServiceValidations.texto(nom, "El nombre");

        Customer cliente = new Customer(id, nom, tel, dir);
        customerDao.guardar(cliente);
        return cliente;
    }

    /**
     * Agrega el rol de cliente a una persona que ya es domiciliario, sin volver
     * a pedir sus datos personales (RN25, SP11).
     *
     * @param identificacion documento de la persona
     * @param direccion      dirección principal de entrega
     * @return el cliente registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si la persona no está registrada como domiciliario
     * @throws IllegalStateException    si ya es cliente
     */
    public Customer asignarRolCliente(String identificacion, String direccion) throws IOException {
        String id = ServiceValidations.texto(identificacion, "La identificación");
        if (personas.domiciliario(id) == null) {
            throw new IllegalArgumentException("No existe una persona registrada con la identificación " + id
                    + ". Use registrarCliente para registrarla con sus datos personales.");
        }
        return registrarCliente(id, null, null, direccion);
    }

    /**
     * Actualiza el nombre, teléfono y dirección principal de un cliente.
     * Los datos personales se actualizan también en su rol de domiciliario, si lo tiene.
     *
     * @param identificacion identificación del cliente a actualizar
     * @param nombre         nuevo nombre
     * @param telefono       nuevo teléfono (obligatorio solo si también es domiciliario)
     * @param direccion      nueva dirección principal
     * @return el cliente con los datos actualizados
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el cliente no existe o algún dato es inválido
     */
    public Customer actualizarCliente(String identificacion, String nombre,
                                      String telefono, String direccion) throws IOException {
        Customer cliente = obtenerPorId(identificacion);
        String dir = ServiceValidations.texto(direccion, "La dirección");
        personas.actualizarDatosPersonales(cliente.getId(), nombre, telefono);

        Customer actualizado = obtenerPorId(cliente.getId());
        actualizado.setAddress(dir);
        customerDao.actualizar(actualizado);
        return actualizado;
    }

    /**
     * Actualiza los datos personales de la persona en todos sus roles (SP11).
     *
     * @param identificacion documento de la persona
     * @param nombre         nuevo nombre
     * @param telefono       nuevo teléfono (obligatorio si es domiciliario, RN26)
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si la persona no existe o algún dato es inválido
     */
    public void actualizarDatosPersonales(String identificacion, String nombre, String telefono) throws IOException {
        personas.actualizarDatosPersonales(identificacion, nombre, telefono);
    }

    /**
     * Lista todos los clientes registrados, con todas sus direcciones.
     *
     * @return lista de clientes (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Customer> listarClientes() throws IOException {
        List<Customer> clientes = customerDao.listar();
        for (Customer c : clientes) {
            cargarDirecciones(c);
        }
        return clientes;
    }

    /**
     * Busca un cliente por su identificación, con todas sus direcciones.
     *
     * @param identificacion identificación a buscar
     * @return el cliente encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Customer buscarPorId(String identificacion) throws IOException {
        if (ServiceValidations.vacio(identificacion)) {
            return null;
        }
        Customer cliente = personas.cliente(identificacion.trim());
        if (cliente != null) {
            cargarDirecciones(cliente);
        }
        return cliente;
    }

    /**
     * Obtiene un cliente por su identificación y falla si no existe (SP01).
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
     * @param identificacion documento de la persona
     * @return {@code true} si la persona también tiene el rol de domiciliario
     * @throws IOException si ocurre un error al leer el archivo
     */
    public boolean esDomiciliario(String identificacion) throws IOException {
        return !ServiceValidations.vacio(identificacion) && personas.domiciliario(identificacion.trim()) != null;
    }

    // ---------------------------------------------------------------- Direcciones

    /**
     * Registra una dirección adicional para un cliente (RN01). El identificador
     * se genera como {@code <idCliente>-DIR-<n>} y es único en el sistema (RN02).
     *
     * @param idCliente       cliente dueño de la dirección
     * @param etiqueta        nombre corto: Casa, Trabajo, Universidad...
     * @param direccionExacta nomenclatura de la dirección
     * @param barrio          barrio (opcional)
     * @param referencia      punto de referencia (opcional)
     * @return la dirección registrada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el cliente no existe o algún dato es inválido
     * @throws IllegalStateException    si el cliente ya tiene una dirección igual
     */
    public Address agregarDireccion(String idCliente, String etiqueta, String direccionExacta,
                                    String barrio, String referencia) throws IOException {
        Customer cliente = obtenerPorId(idCliente);
        String eti = ServiceValidations.texto(etiqueta, "La etiqueta de la dirección");
        String exacta = ServiceValidations.texto(direccionExacta, "La dirección exacta");
        String bar = ServiceValidations.opcional(barrio, "El barrio");
        String ref = ServiceValidations.opcional(referencia, "La referencia");

        for (Address a : cliente.getDirecciones()) {
            if (a.getDireccionExacta().equalsIgnoreCase(exacta) && a.getBarrio().equalsIgnoreCase(bar)) {
                throw new IllegalStateException("El cliente ya tiene registrada esa dirección ("
                        + a.getEtiqueta() + ").");
            }
        }

        String idDireccion = siguienteIdDireccion(cliente);
        Address direccion = new Address(idDireccion, eti, exacta, bar, ref);
        cliente.agregarDireccion(direccion);
        addressDao.guardar(cliente.getId(), direccion);
        return direccion;
    }

    /**
     * Lista todas las direcciones de un cliente; la primera es la principal.
     *
     * @param idCliente cliente a consultar
     * @return direcciones del cliente
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el cliente no existe
     */
    public List<Address> listarDirecciones(String idCliente) throws IOException {
        return obtenerPorId(idCliente).getDirecciones();
    }

    /**
     * Obtiene una dirección validando que pertenezca al cliente (RN02, RN27).
     *
     * @param idCliente   cliente dueño
     * @param idDireccion dirección a consultar
     * @return la dirección
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el cliente no existe o la dirección no es suya
     */
    public Address obtenerDireccion(String idCliente, String idDireccion) throws IOException {
        return obtenerPorId(idCliente).obtenerDireccion(ServiceValidations.texto(idDireccion, "El id de la dirección"));
    }

    /**
     * @param idCliente   cliente
     * @param idDireccion dirección
     * @return {@code true} si la dirección está registrada por ese cliente (RN27)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public boolean esDireccionDelCliente(String idCliente, String idDireccion) throws IOException {
        Customer cliente = buscarPorId(idCliente);
        if (cliente == null || ServiceValidations.vacio(idDireccion)) {
            return false;
        }
        for (Address a : cliente.getDirecciones()) {
            if (a.getId().equals(idDireccion.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Elimina una dirección adicional de un cliente. La dirección principal no se
     * elimina (el cliente debe conservar al menos una); se modifica con
     * {@link #actualizarCliente}.
     *
     * @param idCliente   cliente dueño
     * @param idDireccion dirección a eliminar
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el cliente no existe o la dirección no es suya
     * @throws IllegalStateException    si se intenta eliminar la dirección principal
     */
    public void eliminarDireccion(String idCliente, String idDireccion) throws IOException {
        Customer cliente = obtenerPorId(idCliente);
        Address direccion = cliente.obtenerDireccion(ServiceValidations.texto(idDireccion, "El id de la dirección"));
        if (direccion.equals(cliente.getDireccionPrincipal())) {
            throw new IllegalStateException("No se puede eliminar la dirección principal del cliente.");
        }
        cliente.eliminarDireccion(direccion.getId());
        addressDao.eliminar(direccion.getId());
    }

    /** Agrega al cliente sus direcciones adicionales guardadas. */
    private void cargarDirecciones(Customer cliente) throws IOException {
        for (Address a : addressDao.listarPorCliente(cliente.getId())) {
            if (!cliente.tieneDireccion(a)) {
                cliente.agregarDireccion(a);
            }
        }
    }

    /** Genera un id de dirección que no exista para ningún cliente (RN02, SP08). */
    private String siguienteIdDireccion(Customer cliente) throws IOException {
        int n = cliente.getDirecciones().size() + 1;
        String id;
        do {
            id = cliente.getId() + "-DIR-" + n++;
        } while (contieneDireccion(cliente, id) || addressDao.buscarPropietario(id) != null);
        return id;
    }

    private static boolean contieneDireccion(Customer cliente, String idDireccion) {
        for (Address a : cliente.getDirecciones()) {
            if (a.getId().equalsIgnoreCase(idDireccion)) {
                return true;
            }
        }
        return false;
    }
}
