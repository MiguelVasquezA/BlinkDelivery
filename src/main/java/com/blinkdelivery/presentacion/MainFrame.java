package com.blinkdelivery.presentacion;

import com.blinkdelivery.service.CustomerService;
import com.blinkdelivery.service.DeliveryDriverService;
import com.blinkdelivery.service.OrderService;
import com.blinkdelivery.service.RestaurantService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

/**
 * Ventana principal de BlinkDelivery. Muestra en pestañas de solo lectura
 * los clientes, restaurantes, domiciliarios y pedidos, usando los mismos
 * servicios que la interfaz de consola ({@link ConsoleUI}).
 * <p>
 * Para abrir la interfaz gráfica ejecuta este archivo directamente
 * (método {@link #main(String[])}).
 * </p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CustomerService customerService = new CustomerService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final DeliveryDriverService driverService = new DeliveryDriverService();
    private final OrderService orderService = new OrderService();

    private final DefaultTableModel modeloClientes = nuevoModelo("ID", "Nombre", "Teléfono", "Correo");
    private final DefaultTableModel modeloRestaurantes = nuevoModelo("ID", "Nombre", "Estado", "Dirección");
    private final DefaultTableModel modeloDomiciliarios = nuevoModelo("ID", "Nombre", "Vehículo", "Estado");
    private final DefaultTableModel modeloPedidos = nuevoModelo("ID", "Cliente", "Restaurante", "Estado", "Total");

    private final JTabbedPane pestanas = new JTabbedPane();

    /** Crea la ventana, arma las pestañas y carga los datos actuales. */
    public MainFrame() {
        super("BlinkDelivery - Panel principal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        pestanas.addTab("Clientes", new JScrollPane(new JTable(modeloClientes)));
        pestanas.addTab("Restaurantes", new JScrollPane(new JTable(modeloRestaurantes)));
        pestanas.addTab("Domiciliarios", new JScrollPane(new JTable(modeloDomiciliarios)));
        pestanas.addTab("Pedidos", new JScrollPane(new JTable(modeloPedidos)));

        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> cargarDatos());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.add(actualizar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(pestanas, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);

        setSize(900, 560);
        setLocationRelativeTo(null);
        cargarDatos();
    }

    /** Vuelve a leer todos los datos desde los servicios y refresca las tablas. */
    private void cargarDatos() {
        try {
            llenar(modeloClientes, customerService.listarClientes(), c -> new Object[] {
                    c.getId(), c.getNombre(), c.getTelefono(), c.getEmail() });
            llenar(modeloRestaurantes, restaurantService.listarRestaurantes(), r -> new Object[] {
                    r.getId(), r.getNombre(), r.getEstado(), r.getDireccion() });
            llenar(modeloDomiciliarios, driverService.listarDomiciliarios(), d -> new Object[] {
                    d.getId(), d.getNombre(), d.getTipoVehiculo(), d.getEstado() });
            llenar(modeloPedidos, orderService.listarPedidos(), p -> new Object[] {
                    p.getId(), p.getCustomerId(), p.getRestaurantId(), p.getEstado(), p.getTotal() });
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los datos:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static DefaultTableModel nuevoModelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false; // solo lectura
            }
        };
    }

    /** Filas de una tabla a partir de una lista de elementos. */
    private interface Fila<T> {
        Object[] de(T elemento);
    }

    private static <T> void llenar(DefaultTableModel modelo, List<T> elementos, Fila<T> fila) {
        modelo.setRowCount(0);
        for (T elemento : elementos) {
            modelo.addRow(fila.de(elemento));
        }
    }

    /**
     * Punto de entrada: abre la ventana principal en el hilo de eventos de Swing.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
