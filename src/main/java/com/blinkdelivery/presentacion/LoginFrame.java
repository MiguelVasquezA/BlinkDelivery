package com.blinkdelivery.presentacion;

import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.service.CustomerService;
import com.blinkdelivery.service.DeliveryDriverService;
import com.blinkdelivery.service.RestaurantService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * Pantalla de inicio: el usuario elige su tipo de cuenta (Cliente, Domiciliario
 * o Restaurante) e ingresa su correo o identificación. Los usuarios se buscan
 * en los datos reales del sistema.
 * <p>
 * El administrador no aparece como tarjeta: entra con el correo
 * {@value #ADMIN_EMAIL} o con doble clic sobre el título (modo oculto).
 * </p>
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    static final String ADMIN_EMAIL = "admin@blink.com";

    private enum Rol { CLIENTE, DOMICILIARIO, RESTAURANTE }

    private final CustomerService customerService = new CustomerService();
    private final DeliveryDriverService driverService = new DeliveryDriverService();
    private final RestaurantService restaurantService = new RestaurantService();

    private final JToggleButton opcionCliente = Tema.opcion("Soy Cliente");
    private final JToggleButton opcionDomiciliario = Tema.opcion("Soy Domiciliario");
    private final JToggleButton opcionRestaurante = Tema.opcion("Soy Restaurante");
    private final JTextField usuario = campo();
    private final JPasswordField clave = new JPasswordField(20);
    private boolean modoAdminOculto;

    /** Crea la pantalla de acceso. */
    public LoginFrame() {
        super("BLINK DELIVERY - Acceso al Sistema");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(cabecera(), BorderLayout.NORTH);
        raiz.add(centro(), BorderLayout.CENTER);
        setContentPane(raiz);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcionCliente);
        grupo.add(opcionDomiciliario);
        grupo.add(opcionRestaurante);
        opcionCliente.setSelected(true);

        setMinimumSize(new Dimension(640, 620));
        setSize(700, 700);
        setLocationRelativeTo(null);
    }

    private JPanel cabecera() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.TARJETA);
        barra.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JLabel marca = Tema.titulo("BLINK DELIVERY - Acceso al Sistema");
        // Doble clic en el título activa/desactiva el acceso oculto de administrador
        marca.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modoAdminOculto = !modoAdminOculto;
                    marca.setForeground(modoAdminOculto ? Tema.NEON : Tema.TEXTO);
                }
            }
        });
        marca.setFont(Tema.fuente(java.awt.Font.BOLD, 18));
        barra.add(marca, BorderLayout.WEST);
        return barra;
    }

    private JPanel centro() {
        JPanel tarjeta = Tema.tarjeta();
        tarjeta.setPreferredSize(new Dimension(460, 520));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel titulo = new JLabel("Iniciar Sesión", SwingConstants.CENTER);
        titulo.setFont(Tema.fuente(java.awt.Font.BOLD, 24));
        titulo.setForeground(Tema.TEXTO);
        JLabel ayuda = Tema.subtitulo("Selecciona tu tipo de cuenta para continuar");
        ayuda.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel roles = new JPanel(new GridLayout(1, 3, 10, 0));
        roles.setOpaque(false);
        roles.add(opcionCliente);
        roles.add(opcionDomiciliario);
        roles.add(opcionRestaurante);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);

        c.gridy = 0;
        formulario.add(Tema.subtitulo("Correo o identificación"), c);
        c.gridy = 1;
        formulario.add(usuario, c);
        c.gridy = 2;
        formulario.add(Tema.subtitulo("Contraseña"), c);
        c.gridy = 3;
        clave.setFont(Tema.fuente(java.awt.Font.PLAIN, 13));
        clave.setBorder(usuario.getBorder());
        clave.setCaretColor(Tema.TEXTO);
        formulario.add(clave, c);

        JButton ingresar = Tema.boton("INGRESAR", true);
        ingresar.setPreferredSize(new Dimension(0, 44));
        ingresar.addActionListener(e -> iniciarSesion());
        getRootPane().setDefaultButton(ingresar);

        JButton registro = new JButton("¿No tienes cuenta de cliente? Regístrate");
        registro.setFont(Tema.fuente(java.awt.Font.BOLD, 12));
        registro.setForeground(Tema.NEON);
        registro.setContentAreaFilled(false);
        registro.setBorderPainted(false);
        registro.setFocusPainted(false);
        registro.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        registro.addActionListener(e -> registrarCliente());

        JPanel pie = new JPanel(new GridLayout(0, 1, 0, 10));
        pie.setOpaque(false);
        pie.add(ingresar);
        pie.add(registro);

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(6, 0, 6, 0);
        g.gridy = 0;
        contenido.add(titulo, g);
        g.gridy = 1;
        contenido.add(ayuda, g);
        g.gridy = 2;
        g.insets = new Insets(18, 0, 10, 0);
        contenido.add(roles, g);
        g.gridy = 3;
        g.insets = new Insets(6, 0, 6, 0);
        contenido.add(formulario, g);
        g.gridy = 4;
        g.insets = new Insets(14, 0, 0, 0);
        contenido.add(pie, g);

        tarjeta.add(contenido, BorderLayout.CENTER);

        JPanel envoltura = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        envoltura.setBackground(Tema.FONDO);
        envoltura.add(tarjeta);
        return envoltura;
    }

    private static JTextField campo() {
        JTextField campo = new JTextField(20);
        campo.setFont(Tema.fuente(java.awt.Font.PLAIN, 13));
        campo.setCaretColor(Tema.TEXTO);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return campo;
    }

    private Rol rolSeleccionado() {
        if (opcionDomiciliario.isSelected()) {
            return Rol.DOMICILIARIO;
        }
        if (opcionRestaurante.isSelected()) {
            return Rol.RESTAURANTE;
        }
        return Rol.CLIENTE;
    }

    /** Busca al usuario en los datos reales y abre el panel de su rol. */
    private void iniciarSesion() {
        String dato = usuario.getText().trim();
        try {
            if (modoAdminOculto || dato.equalsIgnoreCase(ADMIN_EMAIL)) {
                abrir(new MainFrame());
                return;
            }
            if (dato.isEmpty()) {
                Tema.aviso(this, "Ingresa tu correo o identificación.");
                return;
            }
            switch (rolSeleccionado()) {
                case CLIENTE -> {
                    Customer cliente = buscarCliente(dato);
                    if (cliente == null) {
                        Tema.aviso(this, "No existe un cliente con ese correo o identificación.\n"
                                + "Puedes registrarte con el enlace de abajo.");
                        return;
                    }
                    abrir(new CustomerFrame(cliente.getId()));
                }
                case DOMICILIARIO -> {
                    DeliveryDriver domiciliario = buscarDomiciliario(dato);
                    if (domiciliario == null) {
                        Tema.aviso(this, "No existe un domiciliario con ese correo o identificación.");
                        return;
                    }
                    abrir(new DriverFrame(domiciliario.getId()));
                }
                case RESTAURANTE -> {
                    Restaurant restaurante = restaurantService.buscarPorId(dato);
                    if (restaurante == null) {
                        Tema.aviso(this, "No existe un restaurante con el ID " + dato + ".");
                        return;
                    }
                    abrir(new RestaurantFrame(restaurante.getId()));
                }
            }
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    private Customer buscarCliente(String dato) throws IOException {
        for (Customer c : customerService.listarClientes()) {
            if (c.getId().equalsIgnoreCase(dato) || dato.equalsIgnoreCase(c.getEmail())) {
                return c;
            }
        }
        return null;
    }

    private DeliveryDriver buscarDomiciliario(String dato) throws IOException {
        for (DeliveryDriver d : driverService.listarDomiciliarios()) {
            if (d.getId().equalsIgnoreCase(dato) || dato.equalsIgnoreCase(d.getEmail())) {
                return d;
            }
        }
        return null;
    }

    /** Registro de un cliente nuevo; luego queda listo para ingresar con su identificación. */
    private void registrarCliente() {
        String[] datos = Formulario.pedir(this, "Registro de cliente",
                new String[] {"Identificación", "Nombre completo", "Teléfono", "Dirección principal"},
                new String[] {null, null, null, null});
        if (datos == null) {
            return;
        }
        try {
            Customer nuevo = customerService.registrarCliente(datos[0], datos[1], datos[2], datos[3]);
            opcionCliente.setSelected(true);
            usuario.setText(nuevo.getId());
            Tema.aviso(this, "Cuenta creada. Ya puedes ingresar con tu identificación.");
        } catch (IOException | RuntimeException ex) {
            Tema.error(this, ex);
        }
    }

    /** Cierra esta pantalla y muestra la ventana indicada. */
    private void abrir(JFrame ventana) {
        ventana.setVisible(true);
        dispose();
    }

    /**
     * Punto de entrada de la aplicación gráfica: muestra la pantalla de acceso.
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
