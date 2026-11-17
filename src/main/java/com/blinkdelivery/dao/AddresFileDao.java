package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Address;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en archivo plano de las direcciones adicionales de los clientes (RN01, RN02).
 * <p>
 * La dirección principal de cada cliente se sigue guardando en {@code customers.txt};
 * este archivo guarda las demás. Formato de cada línea:
 * {@code idDireccion;idCliente;etiqueta;direccionExacta;barrio;ciudad;referencia}.
 * </p>
 *
 * @author BlinkDelivery
 * @version 1.0
 */
public class AddresFileDao {
    private static final String RUTA = "data/addresses.txt";
    private static final int CAMPOS = 7;

    /**
     * Guarda una dirección asociada a un cliente.
     *
     * @param idCliente Cliente dueño de la dirección.
     * @param direccion Dirección a guardar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(String idCliente, Address direccion) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(toRegistro(idCliente, direccion));
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Lista las direcciones adicionales de un cliente.
     *
     * @param idCliente Cliente a consultar.
     * @return Direcciones del cliente (vacía si no tiene adicionales).
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Address> listarPorCliente(String idCliente) throws IOException {
        List<Address> direcciones = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == CAMPOS && p[1].equalsIgnoreCase(idCliente)) {
                direcciones.add(new Address(p[0], p[2], p[3], p[4], p[5], p[6]));
            }
        }
        return direcciones;
    }

    /**
     * Busca el cliente dueño de una dirección.
     *
     * @param idDireccion Identificador de la dirección.
     * @return Id del cliente dueño, o {@code null} si la dirección no existe.
     * @throws IOException Si falla la lectura del archivo.
     */
    public String buscarPropietario(String idDireccion) throws IOException {
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == CAMPOS && p[0].equalsIgnoreCase(idDireccion)) {
                return p[1];
            }
        }
        return null;
    }

    /**
     * Elimina una dirección adicional.
     *
     * @param idDireccion Identificador de la dirección.
     * @return {@code true} si se eliminó alguna línea.
     * @throws IOException Si falla la lectura o escritura del archivo.
     */
    public boolean eliminar(String idDireccion) throws IOException {
        List<String> lineas = new ArrayList<>();
        boolean eliminada = false;
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] p = linea.split(";", -1);
            if (p.length == CAMPOS && p[0].equalsIgnoreCase(idDireccion)) {
                eliminada = true;
            } else {
                lineas.add(linea);
            }
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
        return eliminada;
    }

    private static String toRegistro(String idCliente, Address d) {
        return d.getId() + ";" + idCliente + ";" + d.getEtiqueta() + ";" + d.getDireccionExacta() + ";"
                + d.getBarrio() + ";" + d.getCiudad() + ";" + d.getReferencia();
    }
}
