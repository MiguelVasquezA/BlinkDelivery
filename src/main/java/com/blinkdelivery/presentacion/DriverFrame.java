package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.service.DeliveryDriverService;
import com.blinkdelivery.service.OrderService;

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
import java.util.Locale;

/**
 * Panel del domiciliario: ver los pedidos listos para recoger, aceptar una
 * entrega, avanzarla hasta entregado y cambiar su disponibilidad.
 */
public class DriverFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DeliveryDriverService driverService = new DeliveryDriverService();
    private final OrderService orderService = new OrderService();

    private final String idDomiciliario;
    private DeliveryDriver domiciliario;

    private final JLabel estado = new JLabel();
    private final JButton botonDisponibilidad = Tema.boton("Desconectarme", false);
    private final JLabel tarjetaDisponibles = new JLabel();
    private final JLabel tarjetaActivas = new JLabel();
    private final JLabel tarjetaCompletadas = new JLabel();
    private final JLabel tarjetaCalificacion = new JLabel();

    private final DefaultTableModel modeloDisponibles = Tema.modelo("Pedido", "Restaurante", "Entregar en",
            "Total");
    private final JTable tablaDisponibles = Tema.tabla(modeloDisponibles);
    private final DefaultTableModel modeloEntregas = Tema.modelo("Pedido", "Restaurante", "Cliente",
            "Entregar en", "Estado");
    private final JTable tablaEntregas = Tema.tabla(modeloEntregas);
    private final Timer actualizacion = new Timer(5000, e -> recargarTablas());

    /**
     * @param idDomiciliario identificación del domiciliario que inició sesión
     * @throws IOException si no se pueden leer los datos
     */
    public DriverFrame(String idDomiciliario) throws IOException {
        super("BLINK DELIVERY - Panel del Domiciliario");
        this.idDomiciliario = idDomiciliario;
        this.domiciliario = driverService.obtenerPorId(idDomiciliario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        raiz.add(cabecera(), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Pedidos disponibles", panelDisponibles());
        pestanas.addTab("Mis entregas", panelEntregas());

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
        setSize(1100, 680);
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
        izquierda.add(Tema.titulo("Hola, " + domiciliario.getNombre()));
        izquierda.add(estado);
        barra.add(izquierda, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        botonDisponibilidad.addActionListener(e -> alternarDisponibilidad());
        JButton salir = Tema.boton("Cerrar sesión", false);
        salir.addActionListener(e -> cerrarSesion());
        derecha.add(botonDisponibilidad);
        derecha.add(salir);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel tarjetas() {
        JPanel fila = new JPanel(new GridLayout(1, 4, 14, 0));
        fila.setOpaque(false);
        fila.add(tarjeta("Pedidos para recoger", tarjetaDisponibles));
        fila.add(tarjeta("Entregas activas", tarjetaActivas));
        fila.add(tarjeta("Entregas completadas", tarjetaCompletadas));
        fila.add(tarjeta("Calificación", tarjetaCalificacion));
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

    private JPanel panelDisponibles() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Pedidos listos para recoger"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaDisponibles), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton aceptar = Tema.boton("Aceptar entrega", true);
        aceptar.addActionListener(e -> aceptarEntrega());
        acciones.add(aceptar);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelEntregas() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Tema.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Mis entregas"), BorderLayout.NORTH);
        tarjeta.add(new JScrollPane(tablaEntregas), BorderLayout.CENTER);
        panel.add(tarjeta, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acciones.setOpaque(false);
        JButton enCamino = Tema.boton("Marcar como recogido", false);
        enCamino.addActionListener(e -> cambiarEstadoEntrega(OrderStatus.EN_CAMINO));
        JButton entregado = Tema.boton("Marcar como entregado", true);
        entregado.addActionListener(e -> cambiarEstadoEntrega(OrderStatus.ENTREGADO));
        acciones.add(enCamino);
        acciones.add(entregado);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------------------------- Datos

    private void recargarTodo() {
        try {
            domiciliario = driverService.obtenerPorId(idDomiciliario);
            refrescarEstado();
            recargarTablas();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void refrescarEstado() {
        boolean conectado = domiciliario.getEstado() != DriverStatus.DESCONECTADO;
        estado.setText(conectado ? "● " + domiciliario.getEstado().getDescripcion() : "● Desconectado");
        estado.setFont(Tema.fuente(java.awt.Font.BOLD, 11));
        estado.setForeground(Tema.colorEstado(domiciliario.getEstado()));
        botonDisponibilidad.setText(conectado ? "Desconectarme" : "Conectarme");
        tarjetaCalificacion.setText(String.format(Locale.US, "%.1f / 5", domiciliario.getCalificacionPromedio()));
    }

    /** Vuelve a leer ambas tablas conservando la fila seleccionada en cada una. */
    private void recargarTablas() {
        Tema.conservandoSeleccion(tablaDisponibles, () ->
                Tema.conservandoSeleccion(tablaEntregas, this::cargarTablas));
    }

    private void cargarTablas() {
        try {
            domiciliario = driverService.obtenerPorId(idDomiciliario);
            refrescarEstado();

            modeloDisponibles.setRowCount(0);
            int disponibles = 0;
            for (Order p : orderService.listarPedidos()) {
                if (p.getEstado() == OrderStatus.LISTO_PARA_RECOGER && !p.tieneDomiciliario()) {
                    disponibles++;
                    modeloDisponibles.addRow(new Object[] {p.getId(), p.getRestaurante().getNombre(),
                            p.getDireccionEntrega().getDireccionExacta(), Tema.cop(p.getTotal())});
                }
            }
            tarjetaDisponibles.setText(String.valueOf(disponibles));

            List<Order> entregas = driverService.listarEntregas(idDomiciliario);
            modeloEntregas.setRowCount(0);
            int activas = 0;
            int completadas = 0;
            for (Order p : entregas) {
                if (p.getEstado() == OrderStatus.ENTREGADO) {
                    completadas++;
                } else if (!p.getEstado().esFinal()) {
                    activas++;
                }
                modeloEntregas.addRow(new Object[] {p.getId(), p.getRestaurante().getNombre(),
                        p.getCliente().getNombre(), p.getDireccionEntrega().getDireccionExacta(), p.getEstado()});
            }
            tarjetaActivas.setText(String.valueOf(activas));
            tarjetaCompletadas.setText(String.valueOf(completadas));
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    // ----------------------------------------------------------------- Acciones

    private String seleccion(JTable tabla, DefaultTableModel modelo, String mensaje) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Tema.aviso(this, mensaje);
            return null;
        }
        return (String) modelo.getValueAt(tabla.convertRowIndexToModel(fila), 0);
    }

    private void aceptarEntrega() {
        String id = seleccion(tablaDisponibles, modeloDisponibles, "Selecciona un pedido para recoger.");
        if (id == null) {
            return;
        }
        try {
            orderService.asignarDomiciliario(id, idDomiciliario);
            recargarTablas();
            refrescarEstado();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void cambiarEstadoEntrega(OrderStatus destino) {
        String id = seleccion(tablaEntregas, modeloEntregas, "Selecciona una entrega de la tabla.");
        if (id == null) {
            return;
        }
        try {
            orderService.cambiarEstado(id, destino);
            recargarTablas();
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private void alternarDisponibilidad() {
        try {
            boolean conectar = domiciliario.getEstado() == DriverStatus.DESCONECTADO;
            domiciliario = driverService.cambiarDisponibilidad(idDomiciliario, conectar);
            refrescarEstado();
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
