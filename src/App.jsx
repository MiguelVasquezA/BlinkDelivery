// ============================================================
// App.jsx — Controlador principal: estado de autenticación y roles.
// - Sin sesión -> muestra Login.
// - Con sesión -> muestra el dashboard del rol activo.
// ============================================================
import { useState } from 'react';
import Login from './pages/Login.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import RestaurantDashboard from './pages/RestaurantDashboard.jsx';
import CustomerDashboard from './pages/CustomerDashboard.jsx';
import DriverDashboard from './pages/DriverDashboard.jsx';
import { ROLES } from './data/mockData.js';

// Tabla de rutas: a cada rol le corresponde su panel
const PANELES = {
  [ROLES.ADMIN]: AdminDashboard,
  [ROLES.RESTAURANT]: RestaurantDashboard,
  [ROLES.CUSTOMER]: CustomerDashboard,
  [ROLES.DRIVER]: DriverDashboard,
};

export default function App() {
  // Sesión actual: null (sin login) o { rol, correo }
  const [sesion, setSesion] = useState(null);

  // Se llama desde Login al presionar INGRESAR
  function iniciarSesion(datos) {
    setSesion(datos);
  }

  // Se llama desde el menú de perfil -> "Cerrar Sesión"
  function cerrarSesion() {
    setSesion(null);
  }

  // Sin sesión: obligatoriamente se empieza en el Login
  if (!sesion) {
    return <Login onLogin={iniciarSesion} />;
  }

  // Con sesión: renderiza el panel correspondiente al rol
  const PanelActual = PANELES[sesion.rol];
  return <PanelActual correo={sesion.correo} onLogout={cerrarSesion} />;
}
