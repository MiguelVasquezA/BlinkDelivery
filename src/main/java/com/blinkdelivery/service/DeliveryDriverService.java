package com.blinkdelivery.service;

import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.OrderFileDao;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.Order;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para la gestión de domiciliarios.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>RN25 / SP11: el domiciliario es un rol de la persona; si la persona ya es
 *       cliente se reutilizan sus datos personales con el mismo documento.</li>
 *   <li>RN26: todo domiciliario debe tener teléfono registrado.</li>
 *   <li>RN11: un domiciliario puede hacer muchas entregas a lo largo del tiempo,
 *       pero no cambia de disponibilidad mientras tiene un pedido en curso.</li>
 * </ul>
 */
public class DeliveryDriverService {

    /** DAO que guarda y lee los domiciliarios en el archivo de texto. */
    private final DriverFileDao driverDao = new DriverFileDao();

    /** DAO de clientes, para compartir los datos de la persona entre roles. */
    private final CustomerFileDao customerDao = new CustomerFileDao();

    /** DAO de pedidos, para consultar las entregas del domiciliario. */
    private final OrderFileDao orderDao = new OrderFileDao();

    /** Reglas compartidas de la generalización Persona. */
    private final PersonRoles personas = new PersonRoles(customerDao, driverDao);

    /**
     * Registra el rol de domiciliario de una persona, que queda disponible.
     * <p>
     * Si la persona ya es cliente (mismo documento), el nombre puede dejarse vacío
     * y se reutiliza el registrado; si se informa debe coincidir (SP11). El
     * teléfono es obligatorio (RN26): si el cliente no lo tenía, el informado se
     * guarda también en su rol de cliente para que la persona tenga un solo teléfono.
     * </p>
     *
     * @param identificacion documento de la persona (único)
     * @param nombre         nombre completo (opcional solo si la persona ya existe)
     * @param telefono       teléfono de 7 a 10 dígitos
     * @param tipoVehiculo   tipo de vehículo (por ejemplo, moto o bicicleta)
     * @return el domiciliario registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya es domiciliario o sus datos personales no coinciden
     */
    public DeliveryDriver registrarDomiciliario(String identificacion, String nombre,
                                                String telefono, String tipoVehiculo) throws IOException {
        String id = ServiceValidations.texto(identificacion, "La identificación");
        String vehiculo = ServiceValidations.texto(tipoVehiculo, "El tipo de vehículo");

        if (personas.domiciliario(id) != null) {
            throw new IllegalStateException("Ya existe un domiciliario con la identificación " + id + ".");
        }

        Customer comoCliente = personas.cliente(id);
        String nom;
        String tel;
        if (comoCliente != null) {
            nom = PersonRoles.reutilizar(nombre, comoCliente.getNombre(), "nombre", id);
            tel = PersonRoles.reutilizar(telefono, comoCliente.getTelefono(), "teléfono", id);
        } else {
            nom = ServiceValidations.texto(nombre, "El nombre");
            tel = telefono;
        }
        if (ServiceValidations.vacio(tel)) {
            throw new IllegalArgumentException("El teléfono es obligatorio para los domiciliarios (RN26).");
        }
        tel = ServiceValidations.telefono(tel);
        nom = ServiceValidations.texto(nom, "El nombre");

        DeliveryDriver driver = new DeliveryDriver(id, nom, tel, vehiculo);
        driverDao.guardar(driver);

        if (comoCliente != null && !comoCliente.tieneTelefono()) {
            comoCliente.setTelefono(tel);
            customerDao.actualizar(comoCliente);
        }
        return driver;
    }

    /**
     * Agrega el rol de domiciliario a una persona que ya es cliente, sin volver a
     * pedir su nombre (RN25, SP11).
     *
     * @param identificacion documento de la persona
     * @param telefono       teléfono; puede ir vacío si el cliente ya tenía uno (RN26)
     * @param tipoVehiculo   tipo de vehículo
     * @return el domiciliario registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si la persona no es cliente o falta el teléfono
     * @throws IllegalStateException    si ya es domiciliario
     */
    public DeliveryDriver asignarRolDomiciliario(String identificacion, String telefono,
                                                 String tipoVehiculo) throws IOException {
        String id = ServiceValidations.texto(identificacion, "La identificación");
        if (personas.cliente(id) == null) {
            throw new IllegalArgumentException("No existe una persona registrada con la identificación " + id
                    + ". Use registrarDomiciliario para registrarla con sus datos personales.");
        }
        return registrarDomiciliario(id, null, telefono, tipoVehiculo);
    }

    /**
     * Actualiza los datos personales de la persona en todos sus roles (SP11).
     *
     * @param identificacion documento de la persona
     * @param nombre         nuevo nombre
     * @param telefono       nuevo teléfono (obligatorio, RN26)
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si la persona no existe o algún dato es inválido
     */
    public void actualizarDatosPersonales(String identificacion, String nombre, String telefono) throws IOException {
        personas.actualizarDatosPersonales(identificacion, nombre, telefono);
    }

    /**
     * Cambia el tipo de vehículo de un domiciliario.
     *
     * @param identificacion identificación del domiciliario
     * @param tipoVehiculo   nuevo tipo de vehículo
     * @return el domiciliario actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe o el dato es inválido
     */
    public DeliveryDriver actualizarVehiculo(String identificacion, String tipoVehiculo) throws IOException {
        DeliveryDriver driver = obtenerPorId(identificacion);
        driver.setTipoVehiculo(ServiceValidations.texto(tipoVehiculo, "El tipo de vehículo"));
        driverDao.actualizar(driver);
        return driver;
    }

    /**
     * Lista todos los domiciliarios registrados.
     *
     * @return lista de domiciliarios (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<DeliveryDriver> listarDomiciliarios() throws IOException {
        return driverDao.listar();
    }

    /**
     * Lista los domiciliarios que están disponibles para recibir un pedido.
     *
     * @return lista de domiciliarios disponibles (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<DeliveryDriver> listarDisponibles() throws IOException {
        List<DeliveryDriver> disponibles = new ArrayList<>();
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.estaDisponible()) {
                disponibles.add(d);
            }
        }
        return disponibles;
    }

    /**
     * Obtiene el primer domiciliario disponible.
     *
     * @return el primer domiciliario disponible, o {@code null} si no hay ninguno
     * @throws IOException si ocurre un error al leer el archivo
     */
    public DeliveryDriver obtenerPrimerDisponible() throws IOException {
        List<DeliveryDriver> disponibles = listarDisponibles();
        return disponibles.isEmpty() ? null : disponibles.get(0);
    }

    /**
     * Busca un domiciliario por su identificación.
     *
     * @param identificacion identificación a buscar
     * @return el domiciliario encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public DeliveryDriver buscarPorId(String identificacion) throws IOException {
        return ServiceValidations.vacio(identificacion) ? null : personas.domiciliario(identificacion.trim());
    }

    /**
     * Obtiene un domiciliario por su identificación y falla si no existe.
     *
     * @param identificacion identificación a buscar
     * @return el domiciliario encontrado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe un domiciliario con esa identificación
     */
    public DeliveryDriver obtenerPorId(String identificacion) throws IOException {
        DeliveryDriver driver = buscarPorId(identificacion);
        if (driver == null) {
            throw new IllegalArgumentException("No existe un domiciliario con la identificación " + identificacion + ".");
        }
        return driver;
    }

    /**
     * @param identificacion documento de la persona
     * @return {@code true} si la persona también tiene el rol de cliente
     * @throws IOException si ocurre un error al leer el archivo
     */
    public boolean esCliente(String identificacion) throws IOException {
        return !ServiceValidations.vacio(identificacion) && personas.cliente(identificacion.trim()) != null;
    }

    /**
     * Cambia la disponibilidad de un domiciliario. Quedar no disponible lo pone
     * {@link DriverStatus#DESCONECTADO}. No se permite mientras tenga un pedido en curso:
     * el estado {@link DriverStatus#EN_ENTREGA} lo controla {@link OrderService}.
     *
     * @param identificacion identificación del domiciliario
     * @param disponible     {@code true} si queda disponible, {@code false} si se desconecta
     * @return el domiciliario con la disponibilidad actualizada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el domiciliario
     * @throws IllegalStateException    si tiene un pedido en curso
     */
    public DeliveryDriver cambiarDisponibilidad(String identificacion, boolean disponible) throws IOException {
        DeliveryDriver driver = obtenerPorId(identificacion);
        Order enCurso = pedidoEnCurso(driver.getId());
        if (enCurso != null) {
            throw new IllegalStateException("El domiciliario '" + driver.getNombre()
                    + "' tiene el pedido " + enCurso.getId() + " en curso ('" + enCurso.getEstado() + "').");
        }
        driver.setEstado(disponible ? DriverStatus.DISPONIBLE : DriverStatus.DESCONECTADO);
        driverDao.actualizar(driver);
        return driver;
    }

    /**
     * Lista los pedidos asignados a un domiciliario a lo largo del tiempo (RN11).
     *
     * @param identificacion identificación del domiciliario
     * @return pedidos del domiciliario (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el domiciliario
     */
    public List<Order> listarEntregas(String identificacion) throws IOException {
        DeliveryDriver driver = obtenerPorId(identificacion);
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.tieneDomiciliario() && o.getDomiciliario().getId().equals(driver.getId())) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * @param idDomiciliario domiciliario
     * @return el pedido no finalizado asignado al domiciliario, o {@code null}
     * @throws IOException si ocurre un error al leer el archivo
     */
    private Order pedidoEnCurso(String idDomiciliario) throws IOException {
        for (Order o : orderDao.listar()) {
            if (o.tieneDomiciliario() && o.getDomiciliario().getId().equals(idDomiciliario)
                    && !o.getEstado().esFinal()) {
                return o;
            }
        }
        return null;
    }
}
