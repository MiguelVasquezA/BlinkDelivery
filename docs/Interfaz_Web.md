# BLINK DELIVERY — Interfaz Web

Interfaz web del sistema BLINK DELIVERY con tema oscuro y acentos neón morado/fucsia.
Stack: **React 18 + Vite + Tailwind CSS + Lucide React**. Los gráficos se dibujan con SVG (sin librerías externas).

## Ejecutar

Requisitos: Node.js 18 o superior.

```bash
# desde la raíz del repositorio (donde está package.json)
npm install
npm run dev        # servidor de desarrollo -> http://localhost:5173
```

Otros comandos:

```bash
npm run build      # genera la versión de producción en dist/
npm run preview    # sirve dist/ para revisarla
```

## Cómo probar cada rol

| Acceso | Cómo entrar | Panel |
|---|---|---|
| Cliente | Tarjeta **Soy Cliente** + cualquier correo/clave | Panel del Cliente (Mis Pedidos) |
| Domiciliario | Tarjeta **Soy Domiciliario** + cualquier correo/clave | Panel del Domiciliario |
| Restaurante | Tarjeta **Soy Restaurante** + cualquier correo/clave | Panel del Restaurante Asociado |
| Administrador (oculto) | Correo `admin@blink.com` (cualquier clave) **o** doble clic sobre el logo y luego **INGRESAR** | Panel de Administración |

Para volver al login, abre el menú del perfil (arriba a la derecha) y elige **Cerrar Sesión**.

Credenciales de referencia en `src/data/mockData.js` (`CREDENCIALES_PRUEBA`).

## Estructura de archivos

```
./                             # raíz del repositorio
├── index.html                 # Plantilla HTML (punto de montaje #root)
├── vite.config.js             # Configuración de Vite (plugin React)
├── tailwind.config.js         # Paleta de colores exacta del tema
├── postcss.config.js          # Tailwind + autoprefixer
└── src/
    ├── main.jsx               # Punto de entrada: monta <App />
    ├── index.css              # Estilos globales y botones reutilizables
    ├── App.jsx               # Controlador: sesión y rol activo
    ├── data/
    │   └── mockData.js        # Todos los datos simulados (sin lógica de vista)
    ├── utils/
    │   └── format.js          # Formato de pesos colombianos (COP)
    ├── components/
    │   ├── Logo.jsx           # Logo de la moto morada neón
    │   ├── Sidebar.jsx        # Barra lateral: ítems según el rol
    │   ├── Navbar.jsx         # Título, buscador, iconos y perfil con Cerrar Sesión
    │   └── Widgets.jsx        # Tarjetas, KPI, badges, toggles, gráficos SVG
    └── pages/
        ├── Login.jsx              # Acceso al sistema (+ acceso oculto admin)
        ├── AdminDashboard.jsx     # Panel de Administración
        ├── RestaurantDashboard.jsx# Panel del Restaurante Asociado
        ├── CustomerDashboard.jsx  # Panel del Cliente (Mis Pedidos)
        └── DriverDashboard.jsx    # Panel del Domiciliario
```

## Interactividad (estado local con `useState`)

- **Login:** selección de rol, mostrar/ocultar contraseña, acceso admin oculto.
- **Sidebar:** cambia el ítem activo; los ítems sin contenido muestran un aviso de "en desarrollo".
- **Admin:** selector Diario/Semanal del gráfico, switches de restaurantes.
- **Restaurante:** Aceptar / Rechazar / Marcar listo en pedidos, switches del menú.
- **Cliente:** editar dirección, recargar créditos, ver notificaciones, ver seguimiento de pedidos.
- **Domiciliario:** conectar/desconectar, avanzar la entrega (Recogido → Entregado), aceptar o rechazar solicitudes.
- **Gráficos:** al pasar el mouse sobre un punto de la curva se muestra su valor.

Los datos no se guardan al recargar la página: todo vuelve al estado inicial de `mockData.js`.
