package com.blinkdelivery.service;

import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import java.io.IOException;

/**
 * Lógica compartida de la generalización Persona → Cliente / Domiciliario.
 * <p>
 * Aplica RN25, RN26 y SP11: una misma persona (mismo documento) puede tener uno
 * o ambos roles, sus datos personales (nombre y teléfono) son únicos y se
 * mantienen iguales en los dos roles, y el teléfono es obligatorio solo para
 * el rol de domiciliario.
 * </p>
 */
final class PersonRoles {

    private final CustomerFileDao customerDao;
    private final DriverFileDao driverDao;

    PersonRoles(CustomerFileDao customerDao, DriverFileDao driverDao) {
        this.customerDao = customerDao;
        this.driverDao = driverDao;
    }

    /**
     * @param id documento de la persona
     * @return el rol de cliente de la persona, o {@code null} si no lo tiene
     * @throws IOException si falla la lectura del archivo
     */
    Customer cliente(String id) throws IOException {
        for (Customer c : customerDao.listar()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param id documento de la persona
     * @return el rol de domiciliario de la persona, o {@code null} si no lo tiene
     * @throws IOException si falla la lectura del archivo
     */
    DeliveryDriver domiciliario(String id) throws IOException {
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Resuelve un dato personal cuando la persona ya existe con otro rol (SP11):
     * si no se informa, se reutiliza el registrado; si se informa, debe coincidir.
     *
     * @param suministrado valor recibido (puede ser vacío)
     * @param existente    valor ya registrado para la persona
     * @param campo        nombre del campo, para el mensaje de error
     * @param id           documento de la persona
     * @return el valor que debe usarse
     * @throws IllegalStateException si el valor informado difiere del registrado
     */
    static String reutilizar(String suministrado, String existente, String campo, String id) {
        if (ServiceValidations.vacio(suministrado) || existente == null || existente.isEmpty()) {
            return ServiceValidations.vacio(suministrado) ? existente : suministrado.trim();
        }
        if (!suministrado.trim().equalsIgnoreCase(existente)) {
            throw new IllegalStateException("La persona " + id + " ya está registrada con otro " + campo
                    + " ('" + existente + "'). Los datos personales se registran una sola vez; "
                    + "use actualizarDatosPersonales para modificarlos.");
        }
        return existente;
    }

    /**
     * Actualiza nombre y teléfono en todos los roles que tenga la persona.
     *
     * @param id       documento de la persona
     * @param nombre   nuevo nombre (obligatorio)
     * @param telefono nuevo teléfono (obligatorio si la persona es domiciliario)
     * @throws IOException              si falla la lectura o escritura de los archivos
     * @throws IllegalArgumentException si la persona no existe o algún dato es inválido
     */
    void actualizarDatosPersonales(String id, String nombre, String telefono) throws IOException {
        String doc = ServiceValidations.texto(id, "La identificación");
        String nom = ServiceValidations.texto(nombre, "El nombre");
        Customer cliente = cliente(doc);
        DeliveryDriver driver = domiciliario(doc);
        if (cliente == null && driver == null) {
            throw new IllegalArgumentException("No existe una persona registrada con la identificación " + doc + ".");
        }
        String tel = driver != null
                ? ServiceValidations.telefono(telefono)          // RN26
                : ServiceValidations.telefonoOpcional(telefono);
        if (cliente != null) {
            cliente.setNombre(nom);
            cliente.setTelefono(tel);
            customerDao.actualizar(cliente);
        }
        if (driver != null) {
            driver.setNombre(nom);
            driver.setTelefono(tel);
            driverDao.actualizar(driver);
        }
    }
}
