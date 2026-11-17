package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.service.CustomerService;
import com.blinkdelivery.service.OrderService;
import com.blinkdelivery.service.ProductService;
import com.blinkdelivery.service.RatingService;
import com.blinkdelivery.service.RestaurantService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel del cliente: explorar restaurantes y su menú, armar un carrito, crear
 * pedidos, seguir sus pedidos, calificarlos y administrar sus direcciones.
 */
public class CustomerFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Línea del carrito: un producto con su cantidad. */
    private record Linea(String idProducto, String idRestaurante, String nombre, double precio, int cantidad) {
        double subtotal() {
            return precio * cantidad;
        }
    }

    private final CustomerService customerService = new CustomerService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final RatingService ratingService = new RatingService();

    private final String idCliente;
    private Customer cliente;
    private String restauranteMenu; // id del restaurante cuyo menú se muestra
    private final List<Linea> carrito = new ArrayList<>();
    private final List<Address> direccionesCombo = new ArrayList<>();

    private final JLabel tarjetaDireccion = new JLabel();
    private final JLabel tarjetaEnCurso = new JLabel();
    private final JLabel tarjetaEntregados = new JLabel();

    private final DefaultTableModel modeloRestaurantes = Tema.modelo("ID", "Restaurante", "Cocina", "Calificación");
    private final DefaultTableModel modeloMenu = Tema.modelo("ID", "Producto", "Descripción", "Precio");
    private final DefaultTableModel modeloCarrito = Tema.modelo("Producto", "Restaurante", "Cant.", "Subtotal");
    private final DefaultTableModel modeloPedidos = Tema.modelo("Pedido", "Restaurante", "Estado", "Total", "Fecha");
    private final DefaultTableModel modeloDirecciones = Tema.modelo("ID", "Etiqueta", "Dirección", "Barrio",
            "Referencia");

    private final JTable tablaRestaurantes = Tema.tabla(modeloRestaurantes);
    private final JTable tablaMenu = Tema.tabla(modeloMenu);
    private final JTable tablaCarrito = Tema.tabla(modeloCarrito);
    private final JTable tablaPedidos = Tema.tabla(modeloPedidos);
    private final JTable tablaDirecciones = Tema.tabla(modeloDirecciones);
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JComboBox<String> comboDireccion = new JComboBox<>();
    private final JLabel totalCarrito = new JLabel();
    private final Timer actualizacion = new Timer(5000,
            e -> Tema.conservandoSeleccion(tablaPedidos, this::recargarPedidos));

    /**
     * @param idCliente identificación del cliente que inició sesión
     * @throws IOException si no se pueden leer los datos
     */
    public CustomerFrame(String idCliente) throws IOException {
        super("BLINK DELIVERY - Mis Pedidos");
        this.idCliente = idCliente;
        this.cliente = customerService.obtenerPorId(idCliente); // antes de armar la cabecera
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.add(cabecera(), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Restaurantes", panelRestaurantes());
        pestanas.addTab("Carrito", panelCarrito());
        pestanas.addTab("Mis pedidos", panelPedidos());
        pestanas.addTab("Mis direcciones", panelDirecciones());

        JPanel centro = new JPanel(new BorderLayout(0, 14));
        centro.setOpaque(false);
        centro.add(tarjetas(), BorderLayout.NORTH);
        centro.add(pestanas, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                actualizacion.stop();
            }
        });
        setSize(1100, 700);
        setLocationRelativeTo(null);
        recargarTodo();
        actualizacion.start();
    }

    // ----------------------------------------------------------------- Estructura

    private JPanel cabecera() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(Tema.titulo("Hola, " + cliente.getNombre()), BorderLayout.WEST);
        JButton salir = Tema.boton("Cerrar sesión", false);
        salir.addActionListener(e -> cerrarSesion());
        barra.add(salir, BorderLayout.EAST);
        return barra;
    }

    private JPanel tarjetas() {
        JPanel fila = new JPanel(new GridLayout(1, 3, 14, 0));
        fila.setOpaque(false);
        fila.add(tarjeta("Mi dirección principal", tarjetaDireccion));
        fila.add(tarjeta("Pedidos en curso", tarjetaEnCurso));
        fila.add(tarjeta("Pedidos entregados", tarjetaEntregados));
        return fila;
    }

    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 6));
        tarjeta.add(Tema.subtitulo(titulo), BorderLayout.NORTH);
        valor.setFont(Tema.fuente(java.awt.Font.BOLD, 15));
        valor.setForeground(Tema.TEXTO);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel panelRestaurantes() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel izquierda = Tema.tarjeta();
        izquierda.setLayout(new BorderLayout(0, 8));
        izquierda.add(Tema.seccion("Restaurantes abiertos"), BorderLayout.NORTH);
        izquierda.add(new JScrollPane(tablaRestaurantes), BorderLayout.CENTER);
        izquierda.setPreferredSize(new Dimension(430, 0));
        tablaRestaurantes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tablaRestaurantes.getSelectedRow();
                if (fila >= 0) {
                    restauranteMenu = (String) modeloRestaurantes.getValueAt(
                            tablaRestaurantes.convertRowIndexToModel(fila), 0);
                    recargarMenu();
                }
            }
        });

        JPanel derecha = Tema.tarjeta();
        derecha.setLayout(new BorderLayout(0, 10));
        derecha.add(Tema.seccion("Menú"), BorderLayout.NORTH);
        derecha.add(new JScrollPane(tablaMenu), BorderLayout.CENTER);
        JPanel accion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        accion.setOpaque(false);
        accion.add(Tema.subtitulo("Cantidad"));
        accion.add(cantidad);
        JButton agregar = Tema.boton("Agregar al carrito", true);
        agregar.addActionListener(e -> agregarAlCarrito());
        accion.add(agregar);
        derecha.add(accion, BorderLayout.SOUTH);

        panel.add(izquierda, BorderLayout.WEST);
        panel.add(derecha, BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Mi carrito"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel pie = Tema.tarjeta();
        pie.setLayout(new BorderLayout(10, 0));
        JPanel direccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        direccion.setOpaque(false);
        direccion.add(Tema.subtitulo("Entregar en"));
        comboDireccion.setPreferredSize(new Dimension(340, 30));
        direccion.add(comboDireccion);
        pie.add(direccion, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        totalCarrito.setFont(Tema.fuente(java.awt.Font.BOLD, 15));
        totalCarrito.setForeground(Tema.TEXTO);
        botones.add(totalCarrito);
        JButton quitar = Tema.boton("Quitar", false);
        quitar.addActionListener(e -> quitarDelCarrito());
        JButton vaciar = Tema.boton("Vaciar", false);
        vaciar.addActionListener(e -> {
            carrito.clear();
            refrescarCarrito();
        });
        JButton confirmar = Tema.boton("Confirmar pedido", true);
        confirmar.addActionListener(e -> confirmarPedido());
        botones.add(quitar);
        botones.add(vaciar);
        botones.add(confirmar);
        pie.add(botones, BorderLayout.EAST);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelPedidos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Mis pedidos"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton detalle = Tema.boton("Ver seguimiento", false);
        detalle.addActionListener(e -> verDetalle());
        JButton calificar = Tema.boton("Calificar", true);
        calificar.addActionListener(e -> calificar());
        JButton cancelar = Tema.boton("Cancelar pedido", false);
        cancelar.addActionListener(e -> cancelarPedido());
        acciones.add(detalle);
        acciones.add(calificar);
        acciones.add(cancelar);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelDirecciones() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Mis direcciones"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaDirecciones), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton agregar = Tema.boton("Agregar dirección", true);
        agregar.addActionListener(e -> agregarDireccion());
        JButton eliminar = Tema.boton("Eliminar seleccionada", false);
        eliminar.addActionListener(e -> eliminarDireccion());
        acciones.add(eliminar);
        acciones.add(agregar);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------------------------- Datos

    private void recargarTodo() {
        try {
            cliente = customerService.obtenerPorId(idCliente);
            recargarRestaurantes();
            recargarMenu();
            refrescarCarrito();
            recargarDirecciones();
            recargarPedidos();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void recargarRestaurantes() throws IOException {
        modeloRestaurantes.setRowCount(0);
        for (Restaurant r : restaurantService.listarAbiertos()) {
            modeloRestaurantes.addRow(new Object[] {r.getId(), r.getNombre(), r.getTipoCocina(),
                    String.format(Locale.US, "%.1f / 5", r.getPromedioCalificacion())});
        }
    }

    private void recargarMenu() {
        modeloMenu.setRowCount(0);
        if (restauranteMenu == null) {
            return;
        }
        try {
            for (Product p : productService.listarDisponibles(restauranteMenu)) {
                modeloMenu.addRow(new Object[] {p.getId(), p.getNombre(), p.getDescripcion(), Tema.cop(p.getPrecio())});
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void recargarDirecciones() throws IOException {
        modeloDirecciones.setRowCount(0);
        direccionesCombo.clear();
        comboDireccion.removeAllItems();
        comboDireccion.addItem("Dirección principal");
        Address principal = cliente.getDireccionPrincipal();
        tarjetaDireccion.setText(principal == null ? "Sin dirección" : principal.getDireccionExacta());
        direccionesCombo.add(null);
        for (Address a : customerService.listarDirecciones(idCliente)) {
            modeloDirecciones.addRow(new Object[] {a.getId(), a.getEtiqueta(), a.getDireccionExacta(),
                    a.getBarrio(), a.getReferencia()});
            comboDireccion.addItem(a.getEtiqueta() + " - " + a.getDireccionExacta());
            direccionesCombo.add(a);
        }
    }

    private void recargarPedidos() {
        try {
            List<Order> pedidos = orderService.listarPorCliente(idCliente);
            modeloPedidos.setRowCount(0);
            int enCurso = 0;
            int entregados = 0;
            for (Order p : pedidos) {
                OrderStatus estado = p.getEstado();
                if (estado == OrderStatus.ENTREGADO) {
                    entregados++;
                } else if (!estado.esFinal()) {
                    enCurso++;
                }
                modeloPedidos.addRow(new Object[] {p.getId(), p.getRestaurante().getNombre(), estado,
                        Tema.cop(p.getTotal()), p.getFechaHora().format(FECHA)});
            }
            tarjetaEnCurso.setText(enCurso + (enCurso == 1 ? " pedido activo" : " pedidos activos"));
            tarjetaEntregados.setText(String.valueOf(entregados));
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    // ----------------------------------------------------------------- Acciones

    private void agregarAlCarrito() {
        int fila = tablaMenu.getSelectedRow();
        if (restauranteMenu == null || fila < 0) {
            Tema.aviso(this, "Selecciona un restaurante y un producto del menú.");
            return;
        }
        try {
            String idProducto = (String) modeloMenu.getValueAt(tablaMenu.convertRowIndexToModel(fila), 0);
            Product producto = productService.obtenerDeRestaurante(idProducto, restauranteMenu);
            if (!carrito.isEmpty() && !carrito.get(0).idRestaurante().equals(restauranteMenu)) {
                Tema.aviso(this, "Tu carrito ya tiene productos de otro restaurante.\n"
                        + "Confirma ese pedido o vacía el carrito antes de agregar productos de este.");
                return;
            }
            int cant = (Integer) cantidad.getValue();
            int indice = indiceEnCarrito(producto.getId());
            if (indice >= 0) {
                Linea actual = carrito.get(indice);
                carrito.set(indice, new Linea(actual.idProducto(), actual.idRestaurante(), actual.nombre(),
                        actual.precio(), actual.cantidad() + cant));
            } else {
                carrito.add(new Linea(producto.getId(), restauranteMenu, producto.getNombre(),
                        producto.getPrecio(), cant));
            }
            refrescarCarrito();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private int indiceEnCarrito(String idProducto) {
        for (int i = 0; i < carrito.size(); i++) {
            if (carrito.get(i).idProducto().equals(idProducto)) {
                return i;
            }
        }
        return -1;
    }

    private void quitarDelCarrito() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila >= 0) {
            carrito.remove(tablaCarrito.convertRowIndexToModel(fila));
            refrescarCarrito();
        }
    }

    private void refrescarCarrito() {
        modeloCarrito.setRowCount(0);
        double total = 0;
        for (Linea l : carrito) {
            modeloCarrito.addRow(new Object[] {l.nombre(), restauranteNombre(l.idRestaurante()), l.cantidad(),
                    Tema.cop(l.subtotal())});
            total += l.subtotal();
        }
        totalCarrito.setText("Total: " + Tema.cop(total));
    }

    private String restauranteNombre(String id) {
        try {
            Restaurant r = restaurantService.buscarPorId(id);
            return r == null ? id : r.getNombre();
        } catch (IOException ex) {
            return id;
        }
    }

    private void confirmarPedido() {
        if (carrito.isEmpty()) {
            Tema.aviso(this, "Tu carrito está vacío.");
            return;
        }
        Map<String, Integer> productos = new LinkedHashMap<>();
        for (Linea l : carrito) {
            productos.put(l.idProducto(), l.cantidad());
        }
        int indiceDir = comboDireccion.getSelectedIndex();
        Address direccion = indiceDir <= 0 ? null : direccionesCombo.get(indiceDir);
        try {
            Order pedido = orderService.crearPedido(idCliente, carrito.get(0).idRestaurante(),
                    direccion == null ? null : direccion.getId(), productos);
            carrito.clear();
            refrescarCarrito();
            recargarPedidos();
            Tema.aviso(this, "Pedido " + pedido.getId() + " creado. Total: " + Tema.cop(pedido.getTotal()));
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private String pedidoSeleccionado() {
        int fila = tablaPedidos.getSelectedRow();
        if (fila < 0) {
            Tema.aviso(this, "Selecciona un pedido de la tabla.");
            return null;
        }
        return (String) modeloPedidos.getValueAt(tablaPedidos.convertRowIndexToModel(fila), 0);
    }

    private void verDetalle() {
        String id = pedidoSeleccionado();
        if (id == null) {
            return;
        }
        try {
            new OrderFrame(this, id).setVisible(true);
        } catch (Exception ex) {
            Tema.error(this, ex);
        }
    }

    private void calificar() {
        String id = pedidoSeleccionado();
        if (id == null) {
            return;
        }
        try {
            Order pedido = orderService.obtenerPorId(id);
            if (pedido.getEstado() != OrderStatus.ENTREGADO) {
                Tema.aviso(this, "Solo puedes calificar pedidos entregados.");
                return;
            }
            if (ratingService.estaCalificado(id)) {
                Tema.aviso(this, "Este pedido ya tiene una calificación.");
                return;
            }
            RatingFrame dialogo = new RatingFrame(this, id, idCliente);
            dialogo.setVisible(true);
            recargarPedidos();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void cancelarPedido() {
        String id = pedidoSeleccionado();
        if (id == null) {
            return;
        }
        if (!Tema.confirmar(this, "¿Cancelar el pedido " + id + "?")) {
            return;
        }
        try {
            orderService.cancelarPedido(id);
            recargarPedidos();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void agregarDireccion() {
        String[] datos = Formulario.pedir(this, "Nueva dirección",
                new String[] {"Etiqueta (Casa, Oficina...)", "Dirección exacta", "Barrio", "Referencia"},
                new String[] {null, null, null, null});
        if (datos == null) {
            return;
        }
        try {
            customerService.agregarDireccion(idCliente, datos[0], datos[1], datos[2], datos[3]);
            recargarDirecciones();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void eliminarDireccion() {
        int fila = tablaDirecciones.getSelectedRow();
        if (fila < 0) {
            Tema.aviso(this, "Selecciona una dirección de la tabla.");
            return;
        }
        String id = (String) modeloDirecciones.getValueAt(tablaDirecciones.convertRowIndexToModel(fila), 0);
        if (!Tema.confirmar(this, "¿Eliminar esta dirección?")) {
            return;
        }
        try {
            customerService.eliminarDireccion(idCliente, id);
            recargarDirecciones();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void cerrarSesion() {
        actualizacion.stop();
        new LoginFrame().setVisible(true);
        dispose();
    }
}
