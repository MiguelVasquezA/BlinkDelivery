package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Address;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de direcciones de entrega sobre PostgreSQL (tabla {@code direccion}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/addresses.txt}. El id de la dirección lo
 * asigna la base de datos (columna de identidad); el barrio viene de la migración 009.
 * La dirección principal es la primera que se registró para el cliente.
 * </p>
 *
 * @author BlinkDelivery
 * @version 2.0
 */
public class AddresFileDao {

    /**
     * Guarda una dirección del cliente. Si el id de la dirección es numérico se usa ese;
     * si no, la base de datos asigna uno.
     *
     * @param idCliente id del cliente dueño
     * @param direccion dirección a guardar
     * @return la dirección con el id que quedó en la base de datos
     * @throws IOException si falla la base de datos
     */
    public Address guardar(String idCliente, Address direccion) throws IOException {
        try (Connection c = Db.conectar()) {
            int id = insertar(c, idCliente, direccion, !tienePrincipal(c, Ids.entero(idCliente)));
            return copiaConId(direccion, id);
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar la dirección en la base de datos.", e);
        }
    }

    /**
     * @param idCliente cliente a consultar
     * @return direcciones del cliente, la principal primero (vacía si no tiene)
     * @throws IOException si falla la base de datos
     */
    public List<Address> listarPorCliente(String idCliente) throws IOException {
        if (!Ids.esEntero(idCliente)) {
            return new ArrayList<>();
        }
        try (Connection c = Db.conectar()) {
            return listarPorCliente(c, Ids.entero(idCliente));
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer las direcciones de la base de datos.", e);
        }
    }

    /**
     * @param idDireccion id de la dirección
     * @return id del cliente dueño, o {@code null} si no existe
     * @throws IOException si falla la base de datos
     */
    public String buscarPropietario(String idDireccion) throws IOException {
        if (!Ids.esEntero(idDireccion)) {
            return null;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT id_cliente FROM direccion WHERE id_direccion = ?")) {
            ps.setInt(1, Ids.entero(idDireccion));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? String.valueOf(rs.getInt(1)) : null;
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo consultar la dirección en la base de datos.", e);
        }
    }

    /**
     * Borra una dirección.
     *
     * @param idDireccion id de la dirección
     * @return {@code true} si existía y se borró
     * @throws IOException si falla la base de datos
     */
    public boolean eliminar(String idDireccion) throws IOException {
        if (!Ids.esEntero(idDireccion)) {
            return false;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM direccion WHERE id_direccion = ?")) {
            ps.setInt(1, Ids.entero(idDireccion));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw Db.error("No se pudo eliminar la dirección en la base de datos.", e);
        }
    }

    // ------------------------------------------------------------ auxiliares

    /**
     * Direcciones de un cliente usando una conexión ya abierta (sin abrir otra).
     */
    static List<Address> listarPorCliente(Connection c, int idCliente) throws SQLException {
        List<Address> direcciones = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_direccion, alias, direccion_texto, barrio, ciudad, referencia "
                        + "FROM direccion WHERE id_cliente = ? ORDER BY es_principal DESC, id_direccion")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    direcciones.add(mapear(rs));
                }
            }
        }
        return direcciones;
    }

    /**
     * Todas las direcciones de todos los clientes en una sola consulta, agrupadas por cliente.
     * Evita una consulta por cliente al listar.
     */
    static Map<Integer, List<Address>> listarTodasAgrupadas(Connection c) throws SQLException {
        Map<Integer, List<Address>> porCliente = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_cliente, id_direccion, alias, direccion_texto, barrio, ciudad, referencia "
                        + "FROM direccion ORDER BY id_cliente, es_principal DESC, id_direccion");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                porCliente.computeIfAbsent(rs.getInt("id_cliente"), k -> new ArrayList<>()).add(mapear(rs));
            }
        }
        return porCliente;
    }

    /**
     * Inserta una dirección dentro de una transacción ya abierta y devuelve su id.
     * Usado también por {@link CustomerFileDao} al registrar un cliente.
     */
    static int insertar(Connection c, String idCliente, Address d, boolean principal) throws SQLException {
        String sql = Ids.esEntero(d.getId())
                ? "INSERT INTO direccion (id_direccion, id_cliente, alias, direccion_texto, barrio, ciudad, "
                        + "referencia, es_principal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO direccion (id_cliente, alias, direccion_texto, barrio, ciudad, referencia, "
                        + "es_principal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        boolean conId = Ids.esEntero(d.getId());
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            if (conId) {
                ps.setInt(i++, Ids.entero(d.getId()));
            }
            ps.setInt(i++, Ids.entero(idCliente));
            ps.setString(i++, d.getEtiqueta());
            ps.setString(i++, d.getDireccionExacta());
            ps.setString(i++, emptyToNull(d.getBarrio()));
            ps.setString(i++, d.getCiudad());
            ps.setString(i++, emptyToNull(d.getReferencia()));
            ps.setBoolean(i, principal);
            ps.executeUpdate();
            if (conId) {
                return Ids.entero(d.getId());
            }
            // Sin id explícito, la base asigna la identidad y la devuelve como clave generada.
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("La base no devolvió el id de la dirección nueva.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static boolean tienePrincipal(Connection c, int idCliente) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM direccion WHERE id_cliente = ? AND es_principal")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Address mapear(ResultSet rs) throws SQLException {
        String alias = rs.getString("alias");
        return new Address(String.valueOf(rs.getInt("id_direccion")),
                alias == null || alias.isBlank() ? "Sin etiqueta" : alias,
                rs.getString("direccion_texto"),
                rs.getString("barrio"),
                rs.getString("ciudad"),
                rs.getString("referencia"));
    }

    private static Address copiaConId(Address d, int id) {
        return new Address(String.valueOf(id), d.getEtiqueta(), d.getDireccionExacta(), d.getBarrio(),
                d.getCiudad(), d.getReferencia());
    }

    private static String emptyToNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
