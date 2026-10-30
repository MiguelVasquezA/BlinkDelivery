package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.DeliveryDriver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DriverFileDao {
    private static final String RUTA = "data/drivers.txt";

    public void guardar(DeliveryDriver driver) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(driver.toString());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

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
}
