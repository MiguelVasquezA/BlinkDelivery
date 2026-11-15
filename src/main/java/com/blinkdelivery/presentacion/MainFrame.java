package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.modelo.RestaurantStatus;
import com.blinkdelivery.service.CustomerService;
import com.blinkdelivery.service.DeliveryDriverService;
import com.blinkdelivery.service.OrderService;
import com.blinkdelivery.service.ProductService;
import com.blinkdelivery.service.RestaurantService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Panel del administrador: resumen con métricas clave y gestión (alta, edición
 * y estado) de clientes, restaurantes, domiciliarios, productos y pedidos.
 * <p>
 * Es también el punto de entrada de la aplicación gráfica: {@link #main(String[])}
 * abre la pantalla de acceso ({@link LoginFrame}).
 * </p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CustomerService customerService = new CustomerService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final DeliveryDriverService driverService = new DeliveryDriverService();
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();

    private final JLabel vClientes = valor();
    private final JLabel vRestaurantes = valor();
    private final JLabel vActivos = valor();
    private final JLabel vIngresos = valor();
    private final JLabel vDisponibles = valor();
    private final JLabel vComisiones = valor();

    private final DefaultTableModel mClientes = Tema.modelo("ID", "Nombre", "Teléfono", "Correo", "Dirección");
    private final DefaultTableModel mRestaurantes = Tema.modelo("ID", "Nombre", "Cocina", "Estado", "Calificación");
    private final DefaultTableModel mDomiciliarios = Tema.modelo("ID", "Nombre", "Teléfono", "Vehículo", "Estado");
    private final DefaultTableModel mProductos = Tema.modelo("ID", "Restaurante", "Producto", "Precio",
            "Disponible");
    private final DefaultTableModel mPedidos = Tema.modelo("Pedido", "Cliente", "Restaurante", "Estado", "Total",
            "Domiciliario");

    private final JTable tClientes = Tema.tabla(mClientes);
    private final JTable tRestaurantes = Tema.tabla(mRestaurantes);
    private final JTable tDomiciliarios = Tema.tabla(mDomiciliarios);
    private final JTable tProductos = Tema.tabla(mProductos);
    private final JTable tPedidos = Tema.tabla(mPedidos);

    /** Crea el panel del administrador y carga todos los datos. */
    public MainFrame() {
        super("BLINK DELIVERY - Panel de Administración");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.add(cabecera(), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Resumen", panelResumen());
        pestanas.addTab("Clientes", panelClientes());
        pestanas.addTab("Restaurantes", panelRestaurantes());
        pestanas.addTab("Domiciliarios", panelDomiciliarios());
        pestanas.addTab("Productos", panelProductos());
        pestanas.addTab("Pedidos", panelPedidos());
        raiz.add(pestanas, BorderLayout.CENTER);
        setContentPane(raiz);

        setSize(1180, 740);
        setLocationRelativeTo(null);
        recargarTodo();
    }

    // ----------------------------------------------------------------- Estructura

    private JPanel cabecera() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo("Panel de Administración"), BorderLayout.WEST);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        JButton actualizar = Tema.boton("Actualizar", false);
        actualizar.addActionListener(e -> recargarTodo());
        JButton salir = Tema.boton("Cerrar sesión", false);
        salir.addActionListener(e -> cerrarSesion());
        derecha.add(actualizar);
        derecha.add(salir);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel panelResumen() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 14, 14));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        panel.add(tarjeta("Clientes registrados", vClientes));
        panel.add(tarjeta("Restaurantes", vRestaurantes));
        panel.add(tarjeta("Pedidos activos", vActivos));
        panel.add(tarjeta("Ingresos (pedidos entregados)", vIngresos));
        panel.add(tarjeta("Domiciliarios disponibles", vDisponibles));
        panel.add(tarjeta("Comisiones de la plataforma", vComisiones));
        return panel;
    }

    private static JLabel valor() {
        JLabel etiqueta = new JLabel("—");
        etiqueta.setFont(Tema.fuente(java.awt.Font.BOLD, 26));
        etiqueta.setForeground(Tema.TEXTO);
        return etiqueta;
    }

    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 8));
        tarjeta.add(Tema.subtitulo(titulo), BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    /** Pestaña con una tabla y una barra de botones debajo. */
    private static JPanel pestana(String titulo, JTable tabla, JButton... botones) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion(titulo), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        barra.setOpaque(false);
        for (JButton b : botones) {
            barra.add(b);
        }
        panel.add(barra, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelClientes() {
        JButton nuevo = Tema.boton("Nuevo cliente", true);
        nuevo.addActionListener(e -> nuevoCliente());
        JButton editar = Tema.boton("Editar", false);
        editar.addActionListener(e -> editarCliente());
        return pestana("Clientes", tClientes, editar, nuevo);
    }

    private JPanel panelRestaurantes() {
        JButton nuevo = Tema.boton("Nuevo restaurante", true);
        nuevo.addActionListener(e -> nuevoRestaurante());
        JButton editar = Tema.boton("Editar", false);
        editar.addActionListener(e -> editarRestaurante());
        JButton abrir = Tema.boton("Abrir / cerrar", false);
        abrir.addActionListener(e -> alternarRestaurante());
        return pestana("Restaurantes", tRestaurantes, abrir, editar, nuevo);
    }

    private JPanel panelDomiciliarios() {
        JButton nuevo = Tema.boton("Nuevo domiciliario", true);
        nuevo.addActionListener(e -> nuevoDomiciliario());
        JButton vehiculo = Tema.boton("Cambiar vehículo", false);
        vehiculo.addActionListener(e -> cambiarVehiculo());
        return pestana("Domiciliarios", tDomiciliarios, vehiculo, nuevo);
    }

    private JPanel panelProductos() {
        JButton nuevo = Tema.boton("Nuevo producto", true);
        nuevo.addActionListener(e -> nuevoProducto());
        JButton editar = Tema.boton("Editar", false);
        editar.addActionListener(e -> editarProducto());
        JButton disponible = Tema.boton("Activar / desactivar", false);
        disponible.addActionListener(e -> alternarProducto());
        return pestana("Productos", tProductos, disponible, editar, nuevo);
    }

    private JPanel panelPedidos() {
        JButton detalle = Tema.boton("Ver detalle", false);
        detalle.addActionListener(e -> verDetalle());
        JButton asignar = Tema.boton("Asignar domiciliario automático", true);
        asignar.addActionListener(e -> asignarAutomatico());
        return pestana("Pedidos", tPedidos, detalle, asignar);
    }

    // ----------------------------------------------------------------- Datos

    private void recargarTodo() {
        try {
            cargarResumen();
            cargarClientes();
            cargarRestaurantes();
            cargarDomiciliarios();
            cargarProductos();
            cargarPedidos();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void cargarResumen() throws IOException {
        List<Customer> clientes = customerService.listarClientes();
        List<Restaurant> restaurantes = restaurantService.listarRestaurantes();
        List<Order> pedidos = orderService.listarPedidos();
        long activos = pedidos.stream().filter(p -> !p.getEstado().esFinal()).count();
        double ingresos = pedidos.stream().filter(p -> p.getEstado() == OrderStatus.ENTREGADO)
                .mapToDouble(Order::getTotal).sum();
        vClientes.setText(String.valueOf(clientes.size()));
        vRestaurantes.setText(restaurantes.size() + " (" + restaurantes.stream()
                .filter(r -> r.getEstado() == RestaurantStatus.ABIERTO).count() + " abiertos)");
        vActivos.setText(String.valueOf(activos));
        vIngresos.setText(Tema.cop(ingresos));
        vDisponibles.setText(String.valueOf(driverService.listarDisponibles().size()));
        vComisiones.setText(Tema.cop(orderService.calcularTotalComisiones()));
    }

    private void cargarClientes() throws IOException {
        mClientes.setRowCount(0);
        for (Customer c : customerService.listarClientes()) {
            mClientes.addRow(new Object[] {c.getId(), c.getNombre(), c.getTelefono(), c.getEmail(),
                    c.getDireccionPrincipal() == null ? "" : c.getDireccionPrincipal().getDireccionExacta()});
        }
    }

    private void cargarRestaurantes() throws IOException {
        mRestaurantes.setRowCount(0);
        for (Restaurant r : restaurantService.listarRestaurantes()) {
            mRestaurantes.addRow(new Object[] {r.getId(), r.getNombre(), r.getTipoCocina(), r.getEstado(),
                    String.format(Locale.US, "%.1f / 5", r.getPromedioCalificacion())});
        }
    }

    private void cargarDomiciliarios() throws IOException {
        mDomiciliarios.setRowCount(0);
        for (DeliveryDriver d : driverService.listarDomiciliarios()) {
            mDomiciliarios.addRow(new Object[] {d.getId(), d.getNombre(), d.getTelefono(), d.getTipoVehiculo(),
                    d.getEstado()});
        }
    }

    private void cargarProductos() throws IOException {
        mProductos.setRowCount(0);
        for (Product p : productService.listarProductos()) {
            mProductos.addRow(new Object[] {p.getId(), p.getRestaurantId(), p.getNombre(),
                    Tema.cop(p.getPrecio()), p.isDisponible() ? "Sí" : "No"});
        }
    }

    private void cargarPedidos() throws IOException {
        mPedidos.setRowCount(0);
        for (Order p : orderService.listarPedidos()) {
            mPedidos.addRow(new Object[] {p.getId(), p.getCliente().getNombre(), p.getRestaurante().getNombre(),
                    p.getEstado(), Tema.cop(p.getTotal()),
                    p.tieneDomiciliario() ? p.getDomiciliario().getNombre() : "Sin asignar"});
        }
    }

    // ----------------------------------------------------------------- Utilidades

    /** Id de la fila seleccionada (columna 0) o {@code null} avisando al usuario. */
    private String seleccion(JTable tabla, DefaultTableModel modelo) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Tema.aviso(this, "Selecciona una fila de la tabla.");
            return null;
        }
        return (String) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
    }

    private static double precio(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El precio debe ser un número, por ejemplo 28000.");
        }
    }

    /** Ejecuta una acción que puede fallar por datos inválidos o por el archivo, y recarga todo. */
    private void ejecutar(Accion accion) {
        try {
            accion.correr();
            recargarTodo();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    @FunctionalInterface
    private interface Accion {
        void correr() throws IOException;
    }

    // ----------------------------------------------------------------- Clientes

    private void nuevoCliente() {
        String[] d = Formulario.pedir(this, "Nuevo cliente",
                new String[] {"Identificación", "Nombre completo", "Teléfono", "Dirección principal"},
                new String[4]);
        if (d != null) {
            ejecutar(() -> customerService.registrarCliente(d[0], d[1], d[2], d[3]));
        }
    }

    private void editarCliente() {
        String id = seleccion(tClientes, mClientes);
        if (id == null) {
            return;
        }
        try {
            Customer c = customerService.obtenerPorId(id);
            String[] d = Formulario.pedir(this, "Editar cliente",
                    new String[] {"Identificación", "Nombre completo", "Teléfono", "Dirección principal"},
                    new String[] {c.getId(), c.getNombre(), c.getTelefono(),
                            c.getDireccionPrincipal() == null ? "" : c.getDireccionPrincipal().getDireccionExacta()},
                    0);
            if (d != null) {
                ejecutar(() -> customerService.actualizarCliente(id, d[1], d[2], d[3]));
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    // ----------------------------------------------------------------- Restaurantes

    private void nuevoRestaurante() {
        String[] d = Formulario.pedir(this, "Nuevo restaurante",
                new String[] {"ID del restaurante", "Nombre", "Tipo de cocina / categoría"}, new String[3]);
        if (d != null) {
            ejecutar(() -> restaurantService.registrarRestaurante(d[0], d[1], d[2], List.of(d[2])));
        }
    }

    private void editarRestaurante() {
        String id = seleccion(tRestaurantes, mRestaurantes);
        if (id == null) {
            return;
        }
        try {
            Restaurant r = restaurantService.obtenerPorId(id);
            String[] d = Formulario.pedir(this, "Editar restaurante",
                    new String[] {"ID del restaurante", "Nombre", "Tipo de cocina / categoría"},
                    new String[] {r.getId(), r.getNombre(), r.getTipoCocina()}, 0);
            if (d != null) {
                ejecutar(() -> restaurantService.actualizarRestaurante(id, d[1], d[2]));
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void alternarRestaurante() {
        String id = seleccion(tRestaurantes, mRestaurantes);
        if (id == null) {
            return;
        }
        ejecutar(() -> {
            Restaurant r = restaurantService.obtenerPorId(id);
            restaurantService.cambiarEstado(id, r.getEstado() != RestaurantStatus.ABIERTO);
        });
    }

    // ----------------------------------------------------------------- Domiciliarios

    private void nuevoDomiciliario() {
        String[] d = Formulario.pedir(this, "Nuevo domiciliario",
                new String[] {"Identificación", "Nombre completo", "Teléfono", "Tipo de vehículo"}, new String[4]);
        if (d != null) {
            ejecutar(() -> driverService.registrarDomiciliario(d[0], d[1], d[2], d[3]));
        }
    }

    private void cambiarVehiculo() {
        String id = seleccion(tDomiciliarios, mDomiciliarios);
        if (id == null) {
            return;
        }
        try {
            DeliveryDriver dr = driverService.obtenerPorId(id);
            String[] d = Formulario.pedir(this, "Cambiar vehículo",
                    new String[] {"Identificación", "Tipo de vehículo"},
                    new String[] {dr.getId(), dr.getTipoVehiculo()}, 0);
            if (d != null) {
                ejecutar(() -> driverService.actualizarVehiculo(id, d[1]));
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    // ----------------------------------------------------------------- Productos

    private void nuevoProducto() {
        String[] d = Formulario.pedir(this, "Nuevo producto",
                new String[] {"ID del producto", "ID del restaurante", "Nombre", "Descripción", "Precio (COP)"},
                new String[5]);
        if (d != null) {
            ejecutar(() -> productService.registrarProducto(d[0], d[1], d[2], d[3], precio(d[4])));
        }
    }

    private void editarProducto() {
        String id = seleccion(tProductos, mProductos);
        if (id == null) {
            return;
        }
        try {
            Product p = productService.obtenerPorId(id);
            String[] d = Formulario.pedir(this, "Editar producto",
                    new String[] {"ID del producto", "Nombre", "Descripción", "Precio (COP)"},
                    new String[] {p.getId(), p.getNombre(), p.getDescripcion(), String.valueOf(p.getPrecio())}, 0);
            if (d != null) {
                ejecutar(() -> productService.actualizarProducto(id, d[1], d[2], precio(d[3])));
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void alternarProducto() {
        String id = seleccion(tProductos, mProductos);
        if (id == null) {
            return;
        }
        ejecutar(() -> {
            Product p = productService.obtenerPorId(id);
            productService.cambiarDisponibilidad(id, !p.isDisponible());
        });
    }

    // ----------------------------------------------------------------- Pedidos

    private void verDetalle() {
        String id = seleccion(tPedidos, mPedidos);
        if (id == null) {
            return;
        }
        try {
            new OrderFrame(this, id).setVisible(true);
        } catch (Exception ex) {
            Tema.error(this, ex);
        }
    }

    private void asignarAutomatico() {
        String id = seleccion(tPedidos, mPedidos);
        if (id == null) {
            return;
        }
        ejecutar(() -> orderService.asignarDomiciliarioAutomatico(id));
    }

    private void cerrarSesion() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    /**
     * Punto de entrada de la aplicación gráfica: abre la pantalla de acceso
     * ({@link LoginFrame}) en el hilo de eventos de Swing.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tema.aplicar();
            new LoginFrame().setVisible(true);
        });
    }
}
