package com.blinkdelivery.presentacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * Diálogo modal con varios campos de texto. Se usa para registrar y editar
 * clientes, restaurantes, productos, domiciliarios y direcciones.
 */
public final class Formulario {

    private Formulario() {
    }

    /**
     * Muestra el formulario y espera la respuesta del usuario.
     *
     * @param padre      ventana padre (puede ser null)
     * @param titulo     título del diálogo
     * @param etiquetas  nombre de cada campo
     * @param valores    valor inicial de cada campo (puede contener null)
     * @param bloqueados índices de los campos que no se pueden modificar (p. ej. el id al editar)
     * @return los valores ingresados (sin espacios al inicio o al final), o {@code null} si se canceló
     */
    public static String[] pedir(Component padre, String titulo, String[] etiquetas, String[] valores,
                                 int... bloqueados) {
        Window ventana = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(ventana, titulo, JDialog.ModalityType.APPLICATION_MODAL);
        JTextField[] campos = new JTextField[etiquetas.length];

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(18, 18, 6, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < etiquetas.length; i++) {
            c.gridx = 0;
            c.gridy = i;
            c.weightx = 0;
            formulario.add(Tema.subtitulo(etiquetas[i]), c);

            campos[i] = new JTextField(valores != null && valores[i] != null ? valores[i] : "", 24);
            campos[i].setFont(Tema.fuente(java.awt.Font.PLAIN, 13));
            campos[i].setCaretColor(Tema.TEXTO);
            campos[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.BORDE, 1, true),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            for (int b : bloqueados) {
                if (b == i) {
                    campos[i].setEditable(false);
                    campos[i].setForeground(Tema.APAGADO);
                }
            }
            c.gridx = 1;
            c.weightx = 1;
            formulario.add(campos[i], c);
        }

        final String[][] resultado = {null};
        JButton aceptar = Tema.boton("Guardar", true);
        JButton cancelar = Tema.boton("Cancelar", false);
        aceptar.addActionListener(e -> {
            String[] valoresIngresados = new String[campos.length];
            for (int i = 0; i < campos.length; i++) {
                valoresIngresados[i] = campos[i].getText().trim();
            }
            resultado[0] = valoresIngresados;
            dialogo.dispose();
        });
        cancelar.addActionListener(e -> dialogo.dispose());

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pie.add(cancelar);
        pie.add(aceptar);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.TARJETA);
        raiz.add(formulario, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        raiz.setBackground(Tema.TARJETA);
        dialogo.setContentPane(raiz);
        dialogo.getRootPane().setDefaultButton(aceptar);
        dialogo.pack();
        dialogo.setLocationRelativeTo(ventana);
        dialogo.setVisible(true); // bloquea hasta que se cierre el diálogo
        return resultado[0];
    }
}
