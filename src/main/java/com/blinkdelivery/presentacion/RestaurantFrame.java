package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.modelo.RestaurantStatus;
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
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

/**
 * Panel del restaurante: ver los pedidos entrantes (se actualizan solos),
 * avanzar su estado, abrir o cerrar el local y gestionar el menú.
 */
public class RestaurantFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final RestaurantService restaurantService = new RestaurantService();
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();

    private final String idRestaurante;
    private Restaurant restaurante;

    private final JLabel tarjetaNuevos = new JLabel();
    private final JLabel tarjetaPreparando = new JLabel();
    private final JLabel tarjetaListos = new JLabel();
    private final JLabel tarjetaCalificacion = new JLabel();
    private final JLabel estadoLocal = new JLabel();
    private final JButton botonAbrir = Tema.boton("Abrir local", true);

    private final DefaultTableModel modeloPedidos = Tema.modelo("Pedido", "Cliente", "Productos", "Total",
            "Estado", "Domiciliario");
    private final JTable tablaPedidos = Tema.tabla(modeloPedidos);
    private final DefaultTableModel modeloMenu = Tema.modelo("ID", "Producto", "Descripción", "Precio",
            "Disponible");
    private final JTable tablaMenu = Tema.tabla(modeloMenu);
    private final Timer actualizacion = new Timer(5000,
            e -> Tema.conservandoSeleccion(tablaPedidos, this::recargarPedidos));

    /**
     * @param idRestaurante identificador del restaurante que inició sesión
     * @throws IOException si no se pueden leer los datos
     */
    public RestaurantFrame(String idRestaurante) throws IOException {
        super("BLINK DELIVERY - Panel del Restaurante");
        this.idRestaurante = idRestaurante;
        this.restaurante = restaurantService.obtenerPorId(idRestaurante);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.add(cabecera(), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Pedidos entrantes", panelPedidos());
        pestanas.addTab("Menú", panelMenu());

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
        setSize(1150, 720);
        setLocationRelativeTo(null);
        recargarTodo();
        actualizacion.start();
    }

    // ----------------------------------------------------------------- Estructura

    private JPanel cabecera() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izquierda.setOpaque(false);
        izquierda.add(Tema.titulo("Panel de " + restaurante.getNombre()));
        izquierda.add(estadoLocal);
        barra.add(izquierda, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        botonAbrir.addActionListener(e -> cambiarApertura());
        JButton salir = Tema.boton("Cerrar sesión", false);
        salir.addActionListener(e -> cerrarSesion());
        derecha.add(botonAbrir);
        derecha.add(salir);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel tarjetas() {
        JPanel fila = new JPanel(new GridLayout(1, 4, 14, 0));
        fila.setOpaque(false);
        fila.add(tarjeta("Pedidos nuevos", tarjetaNuevos));
        fila.add(tarjeta("En preparación", tarjetaPreparando));
        fila.add(tarjeta("Listos para recoger", tarjetaListos));
        fila.add(tarjeta("Calificación promedio", tarjetaCalificacion));
        return fila;
    }

    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 6));
        tarjeta.add(Tema.subtitulo(titulo), BorderLayout.NORTH);
        valor.setFont(Tema.fuente(java.awt.Font.BOLD, 18));
        valor.setForeground(Tema.TEXTO);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel panelPedidos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Pedidos activos"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton cancelar = Tema.boton("Rechazar pedido", false);
        cancelar.addActionListener(e -> cambiarEstadoPedido(OrderStatus.CANCELADO));
        JButton aceptar = Tema.boton("Aceptar", false);
        aceptar.addActionListener(e -> cambiarEstadoPedido(OrderStatus.ACEPTADO));
        JButton preparar = Tema.boton("En preparación", false);
        preparar.addActionListener(e -> cambiarEstadoPedido(OrderStatus.EN_PREPARACION));
        JButton listo = Tema.boton("Listo para entregar", true);
        listo.addActionListener(e -> cambiarEstadoPedido(OrderStatus.LISTO_PARA_ENTREGAR));
        acciones.add(cancelar);
        acciones.add(aceptar);
        acciones.add(preparar);
        acciones.add(listo);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelMenu() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Gestión del menú"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaMenu), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton nuevo = Tema.boton("Nuevo producto", true);
        nuevo.addActionListener(e -> nuevoProducto());
        JButton editar = Tema.boton("Editar", false);
        editar.addActionListener(e -> editarProducto());
        JButton disponible = Tema.boton("Activar / desactivar", false);
        disponible.addActionListener(e -> alternarDisponibilidad());
        acciones.add(disponible);
        acciones.add(editar);
        acciones.add(nuevo);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------------------------- Datos

    private void recargarTodo() {
        try {
            restaurante = restaurantService.obtenerPorId(idRestaurante);
            refrescarEstadoLocal();
            recargarPedidos();
            recargarMenu();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void refrescarEstadoLocal() {
        boolean abierto = restaurante.getEstado() == RestaurantStatus.ABIERTO;
        estadoLocal.setFont(Tema.fuente(java.awt.Font.BOLD, 11));
        estadoLocal.setForeground(abierto ? Tema.OK : Tema.ERROR);
        estadoLocal.setText(abierto ? "● ABIERTO" : "● CERRADO");
        botonAbrir.setText(abierto ? "Cerrar local" : "Abrir local");
        tarjetaCalificacion.setText(String.format(java.util.Locale.US, "%.1f / 5",
                restaurante.getPromedioCalificacion()));
    }

    private void recargarPedidos() {
        try {
            List<Order> pedidos = orderService.listarPorRestaurante(idRestaurante);
            modeloPedidos.setRowCount(0);
            int nuevos = 0;
            int preparando = 0;
            int listos = 0;
            for (Order p : pedidos) {
                OrderStatus estado = p.getEstado();
                if (estado.esFinal()) {
                    continue; // solo pedidos activos en esta tabla
                }
                switch (estado) {
                    case RECIBIDO -> nuevos++;
                    case ACEPTADO, EN_PREPARACION -> preparando++;
                    case LISTO_PARA_ENTREGAR -> listos++;
                    default -> { }
                }
                modeloPedidos.addRow(new Object[] {p.getId(), p.getCliente().getNombre(), resumen(p),
                        Tema.cop(p.getTotal()), estado,
                        p.tieneDomiciliario() ? p.getDomiciliario().getNombre() : "Esperando domiciliario"});
            }
            tarjetaNuevos.setText(String.valueOf(nuevos));
            tarjetaPreparando.setText(String.valueOf(preparando));
            tarjetaListos.setText(String.valueOf(listos));
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private static String resumen(Order pedido) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : pedido.getItems()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item.getCantidad()).append("x ").append(item.getProducto().getNombre());
        }
        return sb.toString();
    }

    private void recargarMenu() {
        try {
            modeloMenu.setRowCount(0);
            for (Product p : productService.listarPorRestaurante(idRestaurante)) {
                modeloMenu.addRow(new Object[] {p.getId(), p.getNombre(), p.getDescripcion(),
                        Tema.cop(p.getPrecio()), p.isDisponible() ? "Sí" : "No"});
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    // ----------------------------------------------------------------- Acciones

    private String filaSeleccionada(JTable tabla, DefaultTableModel modelo, String mensaje) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Tema.aviso(this, mensaje);
            return null;
        }
        return (String) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
    }

    private void cambiarEstadoPedido(OrderStatus destino) {
        String id = filaSeleccionada(tablaPedidos, modeloPedidos, "Selecciona un pedido de la tabla.");
        if (id == null) {
            return;
        }
        try {
            orderService.cambiarEstado(id, destino);
            recargarPedidos();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void cambiarApertura() {
        try {
            boolean abrir = restaurante.getEstado() != RestaurantStatus.ABIERTO;
            restaurante = restaurantService.cambiarEstado(idRestaurante, abrir);
            refrescarEstadoLocal();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void nuevoProducto() {
        String[] datos = Formulario.pedir(this, "Nuevo producto",
                new String[] {"ID del producto", "Nombre", "Descripción", "Precio (COP)"},
                new String[] {null, null, null, null});
        if (datos == null) {
            return;
        }
        try {
            productService.registrarProducto(datos[0], idRestaurante, datos[1], datos[2], precio(datos[3]));
            recargarMenu();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void editarProducto() {
        String id = filaSeleccionada(tablaMenu, modeloMenu, "Selecciona un producto del menú.");
        if (id == null) {
            return;
        }
        try {
            Product p = productService.obtenerPorId(id);
            String[] datos = Formulario.pedir(this, "Editar producto",
                    new String[] {"ID del producto", "Nombre", "Descripción", "Precio (COP)"},
                    new String[] {p.getId(), p.getNombre(), p.getDescripcion(), String.valueOf(p.getPrecio())},
                    0);
            if (datos == null) {
                return;
            }
            productService.actualizarProducto(id, datos[1], datos[2], precio(datos[3]));
            recargarMenu();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void alternarDisponibilidad() {
        String id = filaSeleccionada(tablaMenu, modeloMenu, "Selecciona un producto del menú.");
        if (id == null) {
            return;
        }
        try {
            Product p = productService.obtenerPorId(id);
            productService.cambiarDisponibilidad(id, !p.isDisponible());
            recargarMenu();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    /** Convierte el precio escrito por el usuario; acepta coma o punto decimal. */
    private static double precio(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El precio debe ser un número, por ejemplo 28000.");
        }
    }

    private void cerrarSesion() {
        actualizacion.stop();
        new LoginFrame().setVisible(true);
        dispose();
    }
}
