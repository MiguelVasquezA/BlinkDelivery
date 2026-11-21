package com.blinkdelivery.api;

import com.blinkdelivery.dao.CredencialFileDao;
import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Restaurant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea las credenciales de acceso de los perfiles de demostración y muestra un resumen en consola.
 * <p>
 * Lee los 5 clientes, 5 restaurantes y 5 domiciliarios de {@code data/*.txt} (en ese orden) y
 * asigna a cada uno la contraseña {@code cliente1..cliente5}, {@code restaurante1..restaurante5}
 * y {@code domiciliario1..domiciliario5}. Las contraseñas se guardan cifradas (PBKDF2) en
 * {@code data/credenciales.txt}, que está en {@code .gitignore}.
 * </p>
 * <p>
 * Se ejecuta desde la raíz del proyecto: {@code java -Dstdout.encoding=UTF-8 -cp out com.blinkdelivery.api.SembradorCredenciales}.
 * (El flag evita que las tildes del resumen se vean mal en consolas con otra codificación.)
 * Es idempotente: volver a ejecutarlo sustituye las credenciales de estos perfiles.
 * </p>
 */
public final class SembradorCredenciales {

    static final int CUANTOS_POR_ROL = 5;

    private SembradorCredenciales() {
    }

    /** Cuenta de demostración: rol, identificador para ingresar, nombre visible y contraseña en claro. */
    record Cuenta(String rol, String usuario, String nombre, String clave) {
    }

    public static void main(String[] args) throws IOException {
        List<Cuenta> cuentas = cuentasDeDemostracion();
        CredencialFileDao credenciales = new CredencialFileDao();
        for (Cuenta c : cuentas) {
            String[] cifrada = Autenticacion.cifrar(c.clave());
            credenciales.sustituir(c.rol(), c.usuario(), cifrada[0], cifrada[1]);
        }
        imprimirResumen(cuentas);
    }

    /** Lee los perfiles desde los DAO y les asigna contraseñas. Falla si no hay exactamente 5 por rol. */
    static List<Cuenta> cuentasDeDemostracion() throws IOException {
        List<Customer> clientes = new CustomerFileDao().listar();
        List<Restaurant> restaurantes = new RestaurantFileDao().listar();
        List<DeliveryDriver> domiciliarios = new DriverFileDao().listar();
        comprobarCantidad("clientes", clientes.size());
        comprobarCantidad("restaurantes", restaurantes.size());
        comprobarCantidad("domiciliarios", domiciliarios.size());

        List<Cuenta> cuentas = new ArrayList<>();
        for (int i = 0; i < CUANTOS_POR_ROL; i++) {
            Customer c = clientes.get(i);
            cuentas.add(new Cuenta("cliente", c.getIdentification(), c.getNombre(), "cliente" + (i + 1)));
        }
        for (int i = 0; i < CUANTOS_POR_ROL; i++) {
            Restaurant r = restaurantes.get(i);
            cuentas.add(new Cuenta("restaurante", r.getId(), r.getNombre(), "restaurante" + (i + 1)));
        }
        for (int i = 0; i < CUANTOS_POR_ROL; i++) {
            DeliveryDriver d = domiciliarios.get(i);
            cuentas.add(new Cuenta("domiciliario", d.getIdentification(), d.getNombre(), "domiciliario" + (i + 1)));
        }
        return cuentas;
    }

    private static void comprobarCantidad(String nombre, int cantidad) {
        if (cantidad != CUANTOS_POR_ROL) {
            throw new IllegalStateException("Se esperaban exactamente " + CUANTOS_POR_ROL + " " + nombre
                    + " en data/, pero hay " + cantidad + ".");
        }
    }

    private static void imprimirResumen(List<Cuenta> cuentas) {
        System.out.println();
        System.out.println("=== CREDENCIALES DE ACCESO - BlinkDelivery ===");
        System.out.println("Contraseñas guardadas cifradas en data/credenciales.txt (no versionado).");
        imprimirBloque("CLIENTES (ingreso con su identificación)", "cliente", "Identificación", cuentas);
        imprimirBloque("RESTAURANTES (ingreso con el ID del restaurante)", "restaurante", "ID", cuentas);
        imprimirBloque("DOMICILIARIOS (ingreso con su identificación)", "domiciliario", "Identificación", cuentas);
        System.out.println();
    }

    private static void imprimirBloque(String titulo, String rol, String etiquetaUsuario, List<Cuenta> cuentas) {
        System.out.println();
        System.out.println(titulo);
        System.out.printf("  %-14s %-24s %-16s %s%n", etiquetaUsuario, "Nombre", "Contraseña", "Rol");
        for (Cuenta c : cuentas) {
            if (c.rol().equals(rol)) {
                System.out.printf("  %-14s %-24s %-16s %s%n", c.usuario(), c.nombre(), c.clave(), c.rol());
            }
        }
    }
}
