package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.DeliveryDriver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO encarga de la gestión de persistencia en disco de la entidad {@link DeliveryDriver}.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class DriverFileDao {
    private static final String RUTA = "data/drivers.txt";

    /**
     * Guarda un nuevo repartidor en el archivo plano.
     *
     * @param driver Domiciliario a registrar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(DeliveryDriver driver) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(driver.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Lee y construye la lista completa de repartidores guardados.
     *
     * @return Lista con los objetos {@link DeliveryDriver}.
     * @throws IOException Si ocurre un fallo en la lectura.
     */
    public List<DeliveryDriver> listar() throws IOException {
        List<DeliveryDriver> drivers = new ArrayList<>();
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        for (String linea : lineas) {
            String[] partes = linea.split(";");
            if (partes.length == 5) {
                DeliveryDriver d = new DeliveryDriver(partes[0], partes[1], partes[2], partes[3]);
                d.setAvailable(Boolean.parseBoolean(partes[4]));
                drivers.add(d);
            }
        }
        return drivers;
    }

    /**
     * Busca un repartidor específico por su documento de identidad.
     *
     * @param identification Documento a consultar.
     * @return El domiciliario encontrado o {@code null}.
     * @throws IOException Si falla la consulta al archivo.
     */
    public DeliveryDriver buscarPorId(String identification) throws IOException {
        for (DeliveryDriver d : listar()) {
            if (d.getIdentification().equalsIgnoreCase(identification)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Actualiza el estado o atributos de un domiciliario previamente guardado.
     *
     * @param driver Objeto repartidor con los cambios.
     * @throws IOException Si falla al reescribir en archivo.
     */
    public void actualizar(DeliveryDriver driver) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (DeliveryDriver d : listar()) {
            lineas.add(d.getIdentification().equalsIgnoreCase(driver.getIdentification()) ? driver.toString() : d.toString());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }
}