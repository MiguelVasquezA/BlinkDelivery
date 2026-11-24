package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de domiciliarios sobre PostgreSQL (tablas {@code persona} y {@code domiciliario}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/drivers.txt}. Reglas:
 * </p>
 * <ul>
 *   <li>{@code domiciliario.vehiculo} guarda {@code "tipoVehiculo - placa"}; al leer se separa en ambos campos.
 *       Sin placa se guarda {@code "tipoVehiculo - Sin placa"}.</li>
 *   <li>{@code persona} no tiene correo: el de la aplicación es {@code <id>@domiciliario.blinkdelivery.local}.</li>
 *   <li>El teléfono es obligatorio.</li>
 * </ul>
 *
 * @author BlinkDelivery
 * @version 2.0
 */
public class DriverFileDao {
    private static final String SEPARADOR_VEHICULO = " - ";
    private static final String DOMINIO_CORREO = "@domiciliario.blinkdelivery.local";

    private static final String SELECCION = """
            SELECT p.id_persona, p.nombre, p.apellido, p.telefono, d.vehiculo, d.estado
              FROM domiciliario d
              JOIN persona p ON p.id_persona = d.id_domiciliario
            """;

    /**
     * Registra un domiciliario nuevo.
     *
     * @param driver domiciliario a guardar
     * @throws IOException si falta teléfono o falla la base de datos
     */
    public void guardar(DeliveryDriver driver) throws IOException {
        guardarOActualizar(driver);
    }

    /**
     * Actualiza nombre, teléfono, vehículo y estado de un domiciliario.
     *
     * @param driver domiciliario con los datos nuevos
     * @throws IOException si falta teléfono o falla la base de datos
     */
    public void actualizar(DeliveryDriver driver) throws IOException {
        guardarOActualizar(driver);
    }

    /**
     * @return Todos los domiciliarios.
     * @throws IOException si falla la base de datos
     */
    public List<DeliveryDriver> listar() throws IOException {
        List<DeliveryDriver> drivers = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " ORDER BY p.id_persona");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drivers.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer los domiciliarios de la base de datos.", e);
        }
        return drivers;
    }

    /**
     * @param identification documento del domiciliario
     * @return el domiciliario, o {@code null} si no existe o el id no es numérico
     * @throws IOException si falla la base de datos
     */
    public DeliveryDriver buscarPorId(String identification) throws IOException {
        if (!Ids.esEntero(identification)) {
            return null;
        }
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(SELECCION + " WHERE p.id_persona = ?")) {
            ps.setInt(1, Ids.entero(identification));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw Db.error("No se pudo consultar el domiciliario en la base de datos.", e);
        }
    }

    // ------------------------------------------------------------ auxiliares

    private void guardarOActualizar(DeliveryDriver driver) throws IOException {
        int id = Ids.entero(driver.getIdentification());
        String telefono = Personas.telefonoObligatorio(driver.getTelefono());
        String vehiculo = driver.getTipoVehiculo() + SEPARADOR_VEHICULO + driver.getPlacaVehiculo();
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            Personas.guardar(c, id, driver.getNombre(), telefono);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO domiciliario (id_domiciliario, vehiculo, estado, fecha_registro) "
                            + "VALUES (?, ?, ?, CURRENT_DATE) "
                            + "ON CONFLICT (id_domiciliario) DO UPDATE SET vehiculo = EXCLUDED.vehiculo, "
                            + "estado = EXCLUDED.estado")) {
                ps.setInt(1, id);
                ps.setString(2, vehiculo);
                ps.setString(3, driver.getEstado().name());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar el domiciliario en la base de datos.", e);
        }
    }

    private static DeliveryDriver mapear(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("id_persona"));
        String nombre = Personas.unir(rs.getString("nombre"), rs.getString("apellido"));
        String telefono = rs.getString("telefono");
        String vehiculo = rs.getString("vehiculo") == null ? "" : rs.getString("vehiculo");

        String tipo = vehiculo;
        String placa = DeliveryDriver.SIN_PLACA;
        int separador = vehiculo.indexOf(SEPARADOR_VEHICULO);
        if (separador >= 0) {
            tipo = vehiculo.substring(0, separador).trim();
            placa = vehiculo.substring(separador + SEPARADOR_VEHICULO.length()).trim();
        }

        DeliveryDriver d = new DeliveryDriver(id, nombre, telefono == null ? "" : telefono, tipo);
        d.setPlacaVehiculo(placa);
        d.setEstado(DriverStatus.desdeTexto(rs.getString("estado")));
        d.setEmail(id + DOMINIO_CORREO);
        return d;
    }
}
