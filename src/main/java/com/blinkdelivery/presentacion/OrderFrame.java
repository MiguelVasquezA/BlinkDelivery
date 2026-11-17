package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatusChange;
import com.blinkdelivery.service.OrderService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Detalle de un pedido: datos generales, productos, totales y el historial
 * de cambios de estado (RN18). Se abre desde las pantallas de cliente, restaurante,
 * domiciliario y administrador.
 */
public class OrderFrame extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * @param owner    ventana padre
     * @param idPedido pedido a mostrar
     * @throws Exception si el pedido no existe o no se pudo leer
     */
    public OrderFrame(Window owner, String idPedido) throws Exception {
        super(owner, "Pedido " + idPedido, ModalityType.MODELESS);
        OrderService servicio = new OrderService();
        Order pedido = servicio.obtenerPorId(idPedido);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(Tema.titulo("Pedido " + pedido.getId()), BorderLayout.WEST);
        cabecera.add(Tema.badge(pedido.getEstado().getDescripcion(), Tema.colorEstado(pedido.getEstado())),
                BorderLayout.EAST);
        raiz.add(cabecera, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new GridLayout(1, 2, 14, 0));
        cuerpo.setOpaque(false);
        cuerpo.add(datos(pedido));
        cuerpo.add(historial(servicio, pedido));
        raiz.add(cuerpo, BorderLayout.CENTER);

        JButton cerrar = Tema.boton("Cerrar", false);
        cerrar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.setOpaque(false);
        pie.add(cerrar);
        raiz.add(pie, BorderLayout.SOUTH);

        setContentPane(raiz);
        setSize(860, 520);
        setLocationRelativeTo(owner);
    }

    private static JPanel datos(Order pedido) {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));

        StringBuilder texto = new StringBuilder("<html><body style='color:#FFFFFF;font-family:Segoe UI;'>");
        texto.append("<b>Restaurante:</b> ").append(pedido.getRestaurante().getNombre()).append("<br>");
        texto.append("<b>Cliente:</b> ").append(pedido.getCliente().getNombre()).append("<br>");
        texto.append("<b>Entrega:</b> ").append(pedido.getDireccionEntrega().getDireccionExacta()).append("<br>");
        texto.append("<b>Fecha:</b> ").append(pedido.getFechaHora().format(FECHA)).append("<br>");
        texto.append("<b>Domiciliario:</b> ")
                .append(pedido.tieneDomiciliario() ? pedido.getDomiciliario().getNombre() : "Sin asignar")
                .append("<br><br><b>Productos</b><br>");
        for (OrderItem item : pedido.getItems()) {
            texto.append(item.getCantidad()).append(" x ").append(item.getProducto().getNombre())
                    .append(" &nbsp; ").append(Tema.cop(item.getSubtotal())).append("<br>");
        }
        texto.append("<br><b>Total:</b> ").append(Tema.cop(pedido.getTotal()));
        texto.append("</body></html>");

        JLabel etiqueta = new JLabel(texto.toString());
        etiqueta.setFont(Tema.fuente(java.awt.Font.PLAIN, 13));
        etiqueta.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        tarjeta.add(etiqueta, BorderLayout.CENTER);
        return tarjeta;
    }

    private static JPanel historial(OrderService servicio, Order pedido) throws Exception {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new BorderLayout(0, 10));
        tarjeta.add(Tema.seccion("Historial de estados"), BorderLayout.NORTH);

        DefaultTableModel modelo = Tema.modelo("Estado", "Fecha y hora");
        List<OrderStatusChange> cambios = servicio.historialEstados(pedido.getId());
        for (OrderStatusChange cambio : cambios) {
            modelo.addRow(new Object[] {cambio.getEstado(), cambio.getFecha().format(FECHA)});
        }
        JTable tabla = Tema.tabla(modelo);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(300, 300));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tarjeta.add(scroll, BorderLayout.CENTER);
        if (cambios.isEmpty()) {
            tarjeta.add(Tema.subtitulo("Este pedido no tiene historial registrado."), BorderLayout.SOUTH);
        }
        return tarjeta;
    }
}
