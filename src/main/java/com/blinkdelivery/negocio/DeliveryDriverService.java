package com.blinkdelivery.negocio;

import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.modelo.DeliveryDriver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para la gestión de domiciliarios.
 * <p>
 * Permite registrarlos, consultarlos y controlar su disponibilidad
 * delegando la persistencia en {@link DriverFileDao}.
 * </p>
 */
public class DeliveryDriverService {

    /** DAO que guarda y lee los domiciliarios en el archivo de texto. */
    private final DriverFileDao driverDao = new DriverFileDao();

    /**
     * Registra un nuevo domiciliario, que queda disponible por defecto.
     *
     * @param identificacion número de identificación del domiciliario (debe ser único)
     * @param nombre         nombre completo
     * @param telefono       teléfono de contacto (entre 7 y 10 dígitos)
     * @param tipoVehiculo   tipo de vehículo que utiliza (por ejemplo, moto o bicicleta)
     * @return el domiciliario registrado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es vacío o inválido
     * @throws IllegalStateException    si ya existe un domiciliario con esa identificación
     */
    public DeliveryDriver registrarDomiciliario(String identificacion, String nombre,
                                                String telefono, String tipoVehiculo) throws IOException {
        identificacion = texto(identificacion, "La identificación");
        nombre = texto(nombre, "El nombre");
        telefono = validarTelefono(telefono);
        tipoVehiculo = texto(tipoVehiculo, "El tipo de vehículo");

        if (buscarPorId(identificacion) != null) {
            throw new IllegalStateException("Ya existe un domiciliario con la identificación " + identificacion + ".");
        }

        DeliveryDriver driver = new DeliveryDriver(identificacion, nombre, telefono, tipoVehiculo);
        driverDao.guardar(driver);
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
            if (d.isAvailable()) {
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
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.isAvailable()) {
                return d;
            }
        }
        return null;
    }

    /**
     * Busca un domiciliario por su identificación.
     *
     * @param identificacion identificación a buscar
     * @return el domiciliario encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public DeliveryDriver buscarPorId(String identificacion) throws IOException {
        if (identificacion == null) {
            return null;
        }
        String id = identificacion.trim();
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.getIdentification().equals(id)) {
                return d;
            }
        }
        return null;
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
     * Cambia el estado de disponibilidad de un domiciliario.
     *
     * @param identificacion identificación del domiciliario
     * @param disponible     {@code true} si queda disponible, {@code false} si no
     * @return el domiciliario con la disponibilidad actualizada
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el domiciliario
     */
    public DeliveryDriver cambiarDisponibilidad(String identificacion, boolean disponible) throws IOException {
        DeliveryDriver driver = obtenerPorId(identificacion);
        driver.setAvailable(disponible);
        driverDao.actualizar(driver);
        return driver;
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