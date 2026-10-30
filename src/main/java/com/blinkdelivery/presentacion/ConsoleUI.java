package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.negocio.CustomerService;
import com.blinkdelivery.negocio.DeliveryDriverService;
import com.blinkdelivery.negocio.OrderService;
import com.blinkdelivery.negocio.RestaurantService;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de usuario por consola para la aplicación BlinkDelivery.
 * Permite gestionar clientes, restaurantes, productos, domiciliarios y pedidos
 * a través de un menú interactivo.
 *
 * @author Miguel Vasquez & Partner
 * @version 1.0
 */
public class ConsoleUI {

    private static final Scanner scanner = new Scanner(System.in);
    private static final CustomerService customerService = new CustomerService();
    private static final RestaurantService restaurantService = new RestaurantService();
    private static final DeliveryDriverService driverService = new DeliveryDriverService();
    private static final OrderService orderService = new OrderService();

    /**
     * Método principal que arranca el bucle del menú por consola.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        int opcion;
        do {
            mostrarMenuPrincipal();
            opcion = leerEntero("Seleccione una opción: ");

            try {
                switch (opcion) {
                    case 1:
                        menuClientes();
                        break;
                    case 2:
                        menuRestaurantes();
                        break;
                    case 3:
                        menuProductos();
                        break;
                    case 4:
                        menuDomiciliarios();
                        break;
                    case 5:
                        menuPedidos();
                        break;
                    case 0:
                        System.out.println("\n¡Gracias por usar BlinkDelivery! Saliendo del sistema...");
                        break;
                    default:
                        System.out.println("\n[Error] Opción no válida. Intente de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("\n[Error en la operación]: " + e.getMessage());
            }
            System.out.println("\n--------------------------------------------------");
        } while (opcion != 0);
    }

    /** Muestra las opciones principales del sistema en la consola. */
    private static void mostrarMenuPrincipal() {
        System.out.println("\n=== APLICACIÓN DE DOMICILIOS BLINKDELIVERY ===");
        System.out.println("1. Gestionar Clientes");
        System.out.println("2. Gestionar Restaurantes");
        System.out.println("3. Gestionar Productos / Menú");
        System.out.println("4. Gestionar Domiciliarios");
        System.out.println("5. Gestionar Pedidos");
        System.out.println("0. Salir");
    }
    
    private static void menuClientes() throws IOException {
        System.out.println("\n--- GESTIÓN DE CLIENTES ---");
        System.out.println("1. Registrar Cliente");
        System.out.println("2. Listar Clientes");
        int op = leerEntero("Elija una opción: ");

        if (op == 1) {
            System.out.print("Identificación: ");
            String id = scanner.nextLine();
            System.out.print("Nombre completo: ");
            String nombre = scanner.nextLine();
            System.out.print("Teléfono: ");
            String tel = scanner.nextLine();
            System.out.print("Dirección: ");
            String dir = scanner.nextLine();

            Customer c = customerService.registrarCliente(id, nombre, tel, dir);
            System.out.println("\n¡Cliente registrado con éxito! -> " + c.getName());
        } else if (op == 2) {
            List<Customer> lista = customerService.listarClientes();
            System.out.println("\n--- LISTA DE CLIENTES (" + lista.size() + ") ---");
            for (Customer c : lista) {
                System.out.println("- [" + c.getIdentification() + "] " + c.getName() + " | Tel: " + c.getPhone() + " | Dir: " + c.getAddress());
            }
        }
    }

    private static void menuRestaurantes() throws IOException {
        System.out.println("\n--- GESTIÓN DE RESTAURANTES ---");
        System.out.println("1. Registrar Restaurante");
        System.out.println("2. Listar Todos los Restaurantes");
        System.out.println("3. Listar Restaurantes Abiertos");
        int op = leerEntero("Elija una opción: ");

        if (op == 1) {
            System.out.print("ID del restaurante: ");
            String id = scanner.nextLine();
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine();
            System.out.print("Categoría (ej. Comida Rápida, Pizzería): ");
            String cat = scanner.nextLine();

            Restaurant r = restaurantService.registrarRestaurante(id, nombre, cat);
            System.out.println("\n¡Restaurante registrado con éxito! -> " + r.getName());
        } else if (op == 2) {
            List<Restaurant> lista = restaurantService.listarRestaurantes();
            System.out.println("\n--- TODOS LOS RESTAURANTES (" + lista.size() + ") ---");
            for (Restaurant r : lista) {
                System.out.println("- [" + r.getId() + "] " + r.getName() + " (" + r.getCategory() + ") - Estado: " + (r.isOpen() ? "Abierto" : "Cerrado"));
            }
        } else if (op == 3) {
            List<Restaurant> lista = restaurantService.listarAbiertos();
            System.out.println("\n--- RESTAURANTES ABIERTOS ---");
            for (Restaurant r : lista) {
                System.out.println("- [" + r.getId() + "] " + r.getName() + " (" + r.getCategory() + ")");
            }
        }
    }

    private static void menuProductos() throws IOException {
        System.out.println("\n--- GESTIÓN DE PRODUCTOS ---");
        System.out.println("1. Registrar Producto a un Restaurante");
        System.out.println("2. Listar Productos de un Restaurante");
        int op = leerEntero("Elija una opción: ");

        if (op == 1) {
            System.out.print("ID del restaurante propietario: ");
            String id = scanner.nextLine();
            System.out.print("ID del producto: ");
            String idRest = scanner.nextLine();
            System.out.print("Nombre del producto: ");
            String nombre = scanner.nextLine();
            System.out.print("Descripción: ");
            String desc = scanner.nextLine();
            double precio = leerDouble("Precio: ");

            Product p = restaurantService.registrarProducto(idRest, id, nombre, desc, precio);
            System.out.println("\n¡Producto agregado al menú con éxito! -> " + p.getName());
        } else if (op == 2) {
            System.out.print("ID del restaurante a consultar: ");
            String idRest = scanner.nextLine();
            List<Product> lista = restaurantService.listarProductos(idRest);
            System.out.println("\n--- MENÚ DEL RESTAURANTE ---");
            for (Product p : lista) {
                System.out.println("- [" + p.getId() + "] " + p.getName() + " - $" + p.getPrice() + " | " + p.getDescription());
            }
        }
    }

    private static void menuDomiciliarios() throws IOException {
        System.out.println("\n--- GESTIÓN DE DOMICILIARIOS ---");
        System.out.println("1. Registrar Domiciliario");
        System.out.println("2. Listar Domiciliarios");
        int op = leerEntero("Elija una opción: ");

        if (op == 1) {
            System.out.print("Identificación: ");
            String id = scanner.nextLine();
            System.out.print("Nombre completo: ");
            String nombre = scanner.nextLine();
            System.out.print("Teléfono: ");
            String tel = scanner.nextLine();
            System.out.print("Tipo de vehículo (Moto/Bicicleta): ");
            String vehiculo = scanner.nextLine();

            DeliveryDriver d = driverService.registrarDomiciliario(id, nombre, tel, vehiculo);
            System.out.println("\n¡Domiciliario registrado con éxito! -> " + d.getName());
        } else if (op == 2) {
            List<DeliveryDriver> lista = driverService.listarDomiciliarios();
            System.out.println("\n--- LISTA DE DOMICILIARIOS ---");
            for (DeliveryDriver d : lista) {
                System.out.println("- [" + d.getIdentification() + "] " + d.getName() + " (" + d.getVehicleType() + ") - Disp: " + d.isAvailable());
            }
        }
    }

    private static void menuPedidos() throws IOException {
        System.out.println("\n--- GESTIÓN DE PEDIDOS ---");
        System.out.println("1. Crear Nuevo Pedido");
        System.out.println("2. Listar Todos los Pedidos");
        int op = leerEntero("Elija una opción: ");

        if (op == 1) {
            System.out.print("Identificación del cliente: ");
            String idCliente = scanner.nextLine();
            System.out.print("ID del restaurante: ");
            String idRestaurante = scanner.nextLine();
            System.out.print("ID del producto: ");
            String idProducto = scanner.nextLine();
            int cantidad = leerEntero("Cantidad: ");

            Order pedido = orderService.crearPedido(idCliente, idRestaurante, idProducto, cantidad);
            System.out.println("\n¡Pedido creado con éxito!");
            System.out.println("ID Orden: " + pedido.getId() + " | Total a pagar: $" + pedido.getTotal() + " | Estado: " + pedido.getStatus());
        } else if (op == 2) {
            List<Order> lista = orderService.listarPedidos();
            System.out.println("\n--- HISTORIAL DE PEDIDOS ---");
            for (Order o : lista) {
                System.out.println("- Orden ID: " + o.getId() + " | Cliente: " + o.getCustomerId() + " | Total: $" + o.getTotal() + " | Estado: " + o.getStatus());
            }
        }
    }

    private static int leerEntero(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entrada = scanner.nextLine();
                return Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Por favor, ingrese un número entero válido.");
            }
        }
    }

    private static double leerDouble(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entrada = scanner.nextLine();
                return Double.parseDouble(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Por favor, ingrese un número decimal válido.");
            }
        }
    }
}
