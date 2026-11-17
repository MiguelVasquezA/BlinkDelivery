package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.RestaurantStatus;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.Insets;

/**
 * Tema visual de BLINK DELIVERY para Swing: paleta oscura con acentos neón
 * (la misma del diseño web), tipografía y componentes reutilizables.
 */
public final class Tema {

    public static final Color FONDO = new Color(0x0D0D11);
    public static final Color TARJETA = new Color(0x16161D);
    public static final Color BORDE = new Color(0x272732);
    public static final Color NEON = new Color(0xA855F7);
    public static final Color NEON_PROFUNDO = new Color(0x9D00FF);
    public static final Color FUCSIA = new Color(0xD946EF);
    public static final Color OK = new Color(0x22C55E);
    public static final Color ADVERTENCIA = new Color(0xEAB308);
    public static final Color ERROR = new Color(0xEF4444);
    public static final Color APAGADO = new Color(0x9CA3AF);
    public static final Color TEXTO = Color.WHITE;

    private static final String FUENTE = "Segoe UI";

    private Tema() {
    }

    /** Aplica el tema a los componentes Swing; debe llamarse antes de crear ventanas. */
    public static void aplicar() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ignorada) {
            // si falla se usa el look and feel por defecto
        }
        Font base = fuente(Font.PLAIN, 13);
        String[] claves = {"Label.font", "Button.font", "TextField.font", "TextArea.font", "Table.font",
                "TableHeader.font", "ComboBox.font", "TabbedPane.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "Spinner.font", "ToolTip.font", "PasswordField.font"};
        for (String clave : claves) {
            UIManager.put(clave, base);
        }
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Panel.foreground", TEXTO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("OptionPane.background", TARJETA);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("TextField.background", FONDO);
        UIManager.put("TextField.foreground", TEXTO);
        UIManager.put("TextField.caretForeground", TEXTO);
        UIManager.put("PasswordField.background", FONDO);
        UIManager.put("PasswordField.foreground", TEXTO);
        UIManager.put("TextArea.background", FONDO);
        UIManager.put("TextArea.foreground", TEXTO);
        UIManager.put("ComboBox.background", FONDO);
        UIManager.put("ComboBox.foreground", TEXTO);
        UIManager.put("Spinner.background", FONDO);
        UIManager.put("Spinner.foreground", TEXTO);
        UIManager.put("TabbedPane.background", FONDO);
        UIManager.put("TabbedPane.foreground", APAGADO);
        UIManager.put("TabbedPane.selected", TARJETA);
        UIManager.put("TabbedPane.contentAreaColor", FONDO);
        UIManager.put("TabbedPane.light", BORDE);
        UIManager.put("TabbedPane.shadow", BORDE);
        UIManager.put("TabbedPane.darkShadow", BORDE);
        UIManager.put("TabbedPane.focus", NEON);
        UIManager.put("TabbedPane.borderHightlightColor", BORDE);
        UIManager.put("TabbedPane.highlight", BORDE);
        UIManager.put("ScrollPane.background", FONDO);
        UIManager.put("Viewport.background", TARJETA);
        UIManager.put("ScrollBar.background", FONDO);
        UIManager.put("ScrollBar.thumb", BORDE);
        UIManager.put("ScrollBar.track", FONDO);
        UIManager.put("Table.background", TARJETA);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("Table.selectionBackground", NEON_PROFUNDO);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.gridColor", BORDE);
        UIManager.put("TableHeader.background", FONDO);
        UIManager.put("TableHeader.foreground", APAGADO);
        UIManager.put("ToolTip.background", TARJETA);
        UIManager.put("ToolTip.foreground", TEXTO);
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font(FUENTE, estilo, tamano);
    }

    /** Formato de pesos colombianos: 185500 -> "$185.500". */
    public static String cop(double valor) {
        long redondeado = Math.round(valor);
        String digitos = Long.toString(Math.abs(redondeado));
        StringBuilder con = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i > 0 && (digitos.length() - i) % 3 == 0) {
                con.append('.');
            }
            con.append(digitos.charAt(i));
        }
        return (redondeado < 0 ? "-$" : "$") + con;
    }

    // ----------------------------------------------------------------- Textos

    public static JLabel titulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.BOLD, 22));
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    public static JLabel subtitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.PLAIN, 12));
        etiqueta.setForeground(APAGADO);
        return etiqueta;
    }

    public static JLabel seccion(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.BOLD, 15));
        etiqueta.setForeground(TEXTO);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        return etiqueta;
    }

    /** Pastilla de color para un estado (pedido, restaurante o domiciliario). */
    public static JLabel badge(String texto, Color color) {
        return new Badge(texto, color);
    }

    // ----------------------------------------------------------------- Paneles

    /** Panel tipo tarjeta: fondo oscuro, borde redondeado y espaciado interno. */
    public static JPanel tarjeta() {
        JPanel panel = new PanelRedondeado();
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        return panel;
    }

    // ----------------------------------------------------------------- Botones

    /** Botón principal (degradado morado-fucsia) o secundario (borde sutil). */
    public static JButton boton(String texto, boolean primario) {
        return new BotonNeon(texto, primario);
    }

    /** Opción de selección (p. ej. tipo de cuenta) que se ilumina cuando está activa. */
    public static JToggleButton opcion(String texto) {
        return new OpcionNeon(texto);
    }

    // ----------------------------------------------------------------- Tablas

    /** Tabla con el estilo del tema; los estados se pintan con su color. */
    public static JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.setShowVerticalLines(false);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(NEON_PROFUNDO);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFont(fuente(Font.PLAIN, 13));
        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setFont(fuente(Font.BOLD, 12));
        cabecera.setBackground(FONDO);
        cabecera.setForeground(APAGADO);
        cabecera.setReorderingAllowed(false);
        tabla.setDefaultRenderer(Object.class, new CeldaEstado());
        return tabla;
    }

    /**
     * Ejecuta una recarga de la tabla manteniendo seleccionada la misma fila
     * (identificada por el valor de la primera columna), para que el refresco
     * automático no le quite la selección al usuario.
     */
    public static void conservandoSeleccion(JTable tabla, Runnable recarga) {
        int fila = tabla.getSelectedRow();
        Object clave = fila >= 0 ? tabla.getValueAt(fila, 0) : null;
        recarga.run();
        if (clave == null) {
            return;
        }
        for (int i = 0; i < tabla.getRowCount(); i++) {
            if (clave.equals(tabla.getValueAt(i, 0))) {
                tabla.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    /** Modelo de tabla de solo lectura (las celdas no se editan en la tabla). */
    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    // ----------------------------------------------------------------- Colores de estado

    /** Color asociado a un estado del sistema; {@code null} si el valor no es un estado. */
    public static Color colorEstado(Object valor) {
        if (valor instanceof OrderStatus estado) {
            return switch (estado) {
                case RECIBIDO, ACEPTADO -> NEON;
                case EN_PREPARACION -> ADVERTENCIA;
                case LISTO_PARA_RECOGER, ENTREGADO -> OK;
                case EN_CAMINO -> FUCSIA;
                case CANCELADO -> ERROR;
            };
        }
        if (valor instanceof RestaurantStatus estado) {
            return estado == RestaurantStatus.ABIERTO ? OK : ERROR;
        }
        if (valor instanceof DriverStatus estado) {
            return switch (estado) {
                case DISPONIBLE -> OK;
                case EN_ENTREGA -> ADVERTENCIA;
                case DESCONECTADO -> APAGADO;
            };
        }
        return null;
    }

    /** Texto legible para un estado (usa su descripción en español). */
    public static String textoEstado(Object valor) {
        if (valor instanceof OrderStatus estado) {
            return estado.getDescripcion();
        }
        if (valor instanceof RestaurantStatus estado) {
            return estado.getDescripcion();
        }
        if (valor instanceof DriverStatus estado) {
            return estado.getDescripcion();
        }
        return valor == null ? "" : valor.toString();
    }

    // ----------------------------------------------------------------- Avisos

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "BLINK DELIVERY", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, Exception ex) {
        JOptionPane.showMessageDialog(padre, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "BLINK DELIVERY",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ================================================================= Componentes internos

    /** Fondo redondeado con borde sutil, usado por las tarjetas. */
    static class PanelRedondeado extends JPanel {
        private static final long serialVersionUID = 1L;

        PanelRedondeado() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TARJETA);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Botón redondeado; el principal tiene degradado morado-fucsia y resplandor al pasar el mouse. */
    static class BotonNeon extends JButton {
        private static final long serialVersionUID = 1L;
        private final boolean primario;

        BotonNeon(String texto, boolean primario) {
            super(texto);
            this.primario = primario;
            setFont(fuente(Font.BOLD, 13));
            setForeground(TEXTO);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(8, 16, 8, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel modelo = getModel();
            int ancho = getWidth() - 1;
            int alto = getHeight() - 1;
            if (!isEnabled()) {
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
            }
            if (primario) {
                g2.setPaint(new GradientPaint(0, 0, NEON_PROFUNDO, ancho, 0, FUCSIA));
                if (modelo.isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRoundRect(0, 0, ancho, alto, 14, 14);
                }
                g2.fillRoundRect(0, 0, ancho, alto, 14, 14);
            } else {
                g2.setColor(modelo.isRollover() ? new Color(0x1E1E28) : TARJETA);
                g2.fillRoundRect(0, 0, ancho, alto, 14, 14);
                g2.setColor(modelo.isRollover() ? NEON : BORDE);
                g2.drawRoundRect(0, 0, ancho, alto, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Opción de selección con borde morado cuando está activa. */
    static class OpcionNeon extends JToggleButton {
        private static final long serialVersionUID = 1L;

        OpcionNeon(String texto) {
            super(texto);
            setFont(fuente(Font.BOLD, 12));
            setForeground(APAGADO);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setMargin(new Insets(10, 10, 10, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ancho = getWidth() - 1;
            int alto = getHeight() - 1;
            if (isSelected()) {
                g2.setColor(new Color(0x2A1640));
                g2.fillRoundRect(0, 0, ancho, alto, 16, 16);
                g2.setColor(NEON);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, ancho - 2, alto - 2, 16, 16);
                setForeground(TEXTO);
            } else {
                g2.setColor(TARJETA);
                g2.fillRoundRect(0, 0, ancho, alto, 16, 16);
                g2.setColor(BORDE);
                g2.drawRoundRect(0, 0, ancho, alto, 16, 16);
                setForeground(APAGADO);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Pastilla de color (estado). */
    static class Badge extends JLabel {
        private static final long serialVersionUID = 1L;
        private final Color color;

        Badge(String texto, Color color) {
            super(texto);
            this.color = color;
            setFont(fuente(Font.BOLD, 11));
            setForeground(color);
            setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 999, 999);
            g2.setColor(color);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 999, 999);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Celda de tabla: los valores de estado se muestran con su texto y color. */
    static class CeldaEstado extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, textoEstado(valor), seleccionada, foco,
                    fila, columna);
            Color color = colorEstado(valor);
            if (color != null && !seleccionada) {
                c.setForeground(color);
            } else if (!seleccionada) {
                c.setForeground(TEXTO);
            }
            c.setBackground(seleccionada ? NEON_PROFUNDO : TARJETA);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return c;
        }
    }
}
