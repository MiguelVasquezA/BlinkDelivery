package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Customer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de clientes sobre PostgreSQL (tablas {@code persona} y {@code cliente}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/customers.txt}. Reglas:
 * </p>
 * <ul>
 *   <li>El id del cliente es el documento de identidad, guardado como {@code id_persona = id_cliente}.</li>
 *   <li>Teléfono y correo son obligatorios al registrar; si un cliente antiguo no tiene correo,
 *       se muestra {@code sin-correo-<id>@blinkdelivery.local}.</li>
 *   <li>La contraseña vive en {@code cliente.contrasena_hash} (ver {@link CredencialFileDao}).
 *       Un cliente recién registrado queda con hash vacío, es decir, sin contraseña.</li>
 * </ul>
 *
 * @author BlinkDelivery
 * @version 2.0
 */
public class CustomerFileDao {
    private static final String SELECCION = """
            SELECT p.id_persona, p.nombre, p.apellido, p.telefono, c.correo
              FROM cliente c
              JOIN persona p ON p.id_persona = c.id_cliente
            """;

    /**
     * Registra un cliente nuevo con su dirección principal.
     *
     * @param customer cliente a guardar
     * @throws IOException si falta teléfono o correo, o si falla la base de datos
     */
    public void guardar(Customer customer) throws IOException {
        int id = Ids.entero(customer.getIdentification());
        String telefono = Personas.telefonoObligatorio(customer.getTelefono());
        String correo = correoObligatorio(customer.getEmail());
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            Personas.guardar(c, id, customer.getNombre(), telefono);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cliente (id_cliente, correo, contrasena_hash, fecha_registro) "
                            + "VALUES (?, ?, '', CURRENT_DATE)")) {
                ps.setInt(1, id);
                ps.setString(2, correo);
                ps.executeUpdate();
            }
            boolean primera = true;
            for (Address d : customer.getDirecciones()) {
                AddresFileDao.insertar(c, customer.getIdentification(), d, primera);
                primera = false;
            }
            c.commit();
        } catch (SQLException e) {
            throw new IOException("No se pudo guardar el cliente en la base de datos.", e);
        }
    }

    /**
     * @return Todos los clientes, con sus direcciones (la principal primero).
     * @throws IOException si falla la base de datos
     */
    public List<Customer> listar() throws IOException {
        List<Customer> clientes = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " ORDER BY p.id_persona");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(mapear(rs, new AddresFileDao()));
            }
        } catch (SQLException e) {
            throw new IOException("No se pudieron leer los clientes de la base de datos.", e);
        }
        return clientes;
    }

    /**
     * @param identification documento del cliente
     * @return el cliente, o {@code null} si no existe o el id no es numérico
     * @throws IOException si falla la base de datos
     */
    public Customer buscarPorId(String identification) throws IOException {
        if (!Ids.esEntero(identification)) {
            return null;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " WHERE p.id_persona = ?")) {
            ps.setInt(1, Ids.entero(identification));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs, new AddresFileDao()) : null;
            }
        } catch (SQLException e) {
            throw new IOException("No se pudo consultar el cliente en la base de datos.", e);
        }
    }

    /**
     * Actualiza nombre, teléfono y correo. Exige teléfono; si el correo queda vacío
     * conserva el que tenía el cliente o usa el de respaldo.
     *
     * @param customer cliente con los datos nuevos
     * @throws IOException si falta teléfono, o si falla la base de datos
     */
    public void actualizar(Customer customer) throws IOException {
        int id = Ids.entero(customer.getIdentification());
        String telefono = Personas.telefonoObligatorio(customer.getTelefono());
        String correo = Personas.correoPorDefecto(customer.getEmail(), String.valueOf(id), null);
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            Personas.guardar(c, id, customer.getNombre(), telefono);
            try (PreparedStatement ps = c.prepareStatement("UPDATE cliente SET correo = ? WHERE id_cliente = ?")) {
                ps.setString(1, correo);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new IOException("No se pudo actualizar el cliente en la base de datos.", e);
        }
    }

    // ------------------------------------------------------------ auxiliares

    private static Customer mapear(ResultSet rs, AddresFileDao direcciones) throws SQLException, IOException {
        String id = String.valueOf(rs.getInt("id_persona"));
        String nombre = Personas.unir(rs.getString("nombre"), rs.getString("apellido"));
        String telefono = rs.getString("telefono");
        String correo = Personas.correoPorDefecto(rs.getString("correo"), id, null);
        List<Address> lista = direcciones.listarPorCliente(id);
        if (lista.isEmpty()) {
            // Cliente sin dirección en la base: se usa un marcador para no romper el modelo.
            lista = List.of(new Address(id + "-SIN-DIR", "Principal", "Sin dirección", null,
                    Address.CIUDAD_POR_DEFECTO, null));
        }
        Customer cliente = new Customer(id, nombre, telefono == null ? "" : telefono, correo, lista.get(0));
        for (int i = 1; i < lista.size(); i++) {
            cliente.agregarDireccion(lista.get(i));
        }
        return cliente;
    }

    private static String correoObligatorio(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        return correo.trim();
    }
}
