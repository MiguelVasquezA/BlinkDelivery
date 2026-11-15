package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Rating;
import com.blinkdelivery.service.RatingService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Diálogo para calificar un pedido entregado (RN12). Guarda la calificación
 * con {@link RatingService#registrarCalificacion(String, String, int, String)}.
 */
public class RatingFrame extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JComboBox<Integer> estrellas = new JComboBox<>();
    private final JTextArea comentario = new JTextArea(4, 30);
    private final String idPedido;
    private final String idCliente;
    private final RatingService servicio = new RatingService();
    private boolean guardado;

    /**
     * @param owner     ventana padre
     * @param idPedido  pedido entregado a calificar
     * @param idCliente cliente que califica
     */
    public RatingFrame(Window owner, String idPedido, String idCliente) {
        super(owner, "Calificar pedido " + idPedido, ModalityType.APPLICATION_MODAL);
        this.idPedido = idPedido;
        this.idCliente = idCliente;

        for (int i = Rating.MAX_ESTRELLAS; i >= Rating.MIN_ESTRELLAS; i--) {
            estrellas.addItem(i);
        }
        estrellas.setFont(Tema.fuente(java.awt.Font.PLAIN, 14));
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        comentario.setBackground(Tema.FONDO);
        comentario.setForeground(Tema.TEXTO);
        comentario.setCaretColor(Tema.TEXTO);
        comentario.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setLayout(new GridLayout(0, 1, 0, 8));
        tarjeta.add(Tema.seccion("¿Cómo fue tu pedido " + idPedido + "?"));
        tarjeta.add(Tema.subtitulo("Puntuación (1 a 5 estrellas)"));
        tarjeta.add(estrellas);
        tarjeta.add(Tema.subtitulo("Comentario (opcional)"));
        tarjeta.add(comentario);

        JButton cancelar = Tema.boton("Cancelar", false);
        cancelar.addActionListener(e -> dispose());
        JButton enviar = Tema.boton("Enviar calificación", true);
        enviar.addActionListener(e -> enviar());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pie.setOpaque(false);
        pie.add(cancelar);
        pie.add(enviar);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        raiz.add(tarjeta, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(enviar);
        pack();
        setLocationRelativeTo(owner);
    }

    private void enviar() {
        try {
            servicio.registrarCalificacion(idPedido, idCliente, (Integer) estrellas.getSelectedItem(),
                    comentario.getText().trim());
            guardado = true;
            Tema.aviso(this, "¡Gracias por tu calificación!");
            dispose();
        } catch (Exception ex) {
            Tema.error(this, ex);
        }
    }

    /** @return {@code true} si la calificación se guardó. */
    public boolean isGuardado() {
        return guardado;
    }
}
