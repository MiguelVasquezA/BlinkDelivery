// ============================================================
// mockData.js — Datos simulados de BLINK DELIVERY
// Todo el contenido que muestra la interfaz vive aquí, separado
// de las vistas. Montos en COP (pesos colombianos).
// ============================================================

// Identificadores internos de cada rol del sistema
export const ROLES = {
  ADMIN: 'admin',
  RESTAURANT: 'restaurant',
  CUSTOMER: 'customer',
  DRIVER: 'driver',
};

// Correo fijo del administrador (acceso oculto, no aparece como tarjeta)
export const ADMIN_EMAIL = 'admin@blink.com';

// Credenciales de prueba para probar cada rol.
// Nota: el login acepta cualquier correo/contraseña en las 3 tarjetas visibles;
// estas credenciales son solo una referencia para el equipo.
export const CREDENCIALES_PRUEBA = [
  { rol: ROLES.ADMIN, correo: ADMIN_EMAIL, clave: 'admin123', nota: 'Oculto: entra con este correo o con doble clic en el logo' },
  { rol: ROLES.CUSTOMER, correo: 'cliente@blink.com', clave: 'cliente123', nota: 'Soy Cliente' },
  { rol: ROLES.DRIVER, correo: 'domiciliario@blink.com', clave: 'driver123', nota: 'Soy Domiciliario' },
  { rol: ROLES.RESTAURANT, correo: 'restaurante@blink.com', clave: 'resto123', nota: 'Soy Restaurante' },
];

// Menú lateral por rol: cada rol ve sus propios ítems
export const MENUS = {
  [ROLES.ADMIN]: ['Inicio', 'Conductores', 'Restaurantes', 'Pedidos', 'Análisis', 'Finanzas', 'Configuración'],
  [ROLES.RESTAURANT]: ['Inicio', 'Conductores', 'Restaurantes', 'Pedidos', 'Análisis', 'Finanzas', 'Configuración'],
  [ROLES.CUSTOMER]: ['Inicio', 'Favoritos', 'Mis Pedidos', 'Promociones', 'Análisis', 'Cuenta', 'Configuración'],
  [ROLES.DRIVER]: ['Inicio', 'Disponibilidad', 'Mis Entregas', 'Ganancias', 'Soporte', 'Cuenta', 'Configuración'],
};

// ------------------------------------------------------------
// PANEL DE ADMINISTRACIÓN
// ------------------------------------------------------------
export const METRICAS_ADMIN = {
  ingresosTotales: 145890200,
  variacionIngresos: 12.4,
  pedidosCompletados: 12450,
  variacionPedidos: 8.5,
  conductoresActivos: 148,
  conductoresTotales: 180,
  restaurantesAsociados: 84,
};

// Ventas semanales (escala 0 a 200.000)
export const VENTAS_SEMANA_ADMIN = [
  { etiqueta: 'Lun', valor: 120000 },
  { etiqueta: 'Mar', valor: 145000 },
  { etiqueta: 'Mié', valor: 132000 },
  { etiqueta: 'Jue', valor: 168000 },
  { etiqueta: 'Vie', valor: 185000 },
  { etiqueta: 'Sáb', valor: 196000 },
  { etiqueta: 'Dom', valor: 158000 },
];

// Ventas del día por franja horaria (vista "Diario")
export const VENTAS_DIA_ADMIN = [
  { etiqueta: '8h', valor: 12000 },
  { etiqueta: '10h', valor: 35000 },
  { etiqueta: '12h', valor: 98000 },
  { etiqueta: '14h', valor: 76000 },
  { etiqueta: '16h', valor: 54000 },
  { etiqueta: '18h', valor: 142000 },
  { etiqueta: '20h', valor: 176000 },
  { etiqueta: '22h', valor: 118000 },
];

// Pedidos por categoría (dona)
export const CATEGORIAS_PEDIDOS = [
  { etiqueta: 'Restaurantes', valor: 70, color: '#A855F7' },
  { etiqueta: 'Supermercados', valor: 20, color: '#EC4899' },
  { etiqueta: 'Otros', valor: 10, color: '#D8B4FE' },
];

// Conductores en vivo
export const CONDUCTORES_ACTIVOS = [
  { id: 1, nombre: 'Carlos Mendoza', enEntrega: true, pedido: '#4920' },
  { id: 2, nombre: 'Andrés Torres', enEntrega: false },
  { id: 3, nombre: 'Luis Gómez', enEntrega: true, pedido: '#4921' },
  { id: 4, nombre: 'Mateo Rojas', enEntrega: false },
];

// Restaurantes con interruptor de estado
export const RESTAURANTES_ADMIN = [
  { id: 1, nombre: 'Pizzas El Cesar', activo: true, ventasHoy: '9.4k COP hoy' },
  { id: 2, nombre: 'Hamburguesas El Corral', activo: true },
  { id: 3, nombre: 'Pollo Fiesta', activo: true },
  { id: 4, nombre: 'Tacos del Valle', activo: false },
];

// ------------------------------------------------------------
// PANEL DEL RESTAURANTE ASOCIADO
// ------------------------------------------------------------
export const METRICAS_RESTAURANTE = {
  ingresos: 28550400,
  variacionIngresos: 9.8,
  pedidosHoy: 94,
  variacionPedidos: 5.2,
  valorPromedio: 30375,
  itemsVendidos: 315,
};

// Ventas semanales del restaurante (escala 0 a 8.000.000)
export const VENTAS_SEMANA_RESTAURANTE = [
  { etiqueta: 'Lun', valor: 3200000 },
  { etiqueta: 'Mar', valor: 4100000 },
  { etiqueta: 'Mié', valor: 3800000 },
  { etiqueta: 'Jue', valor: 5600000 },
  { etiqueta: 'Vie', valor: 6900000 },
  { etiqueta: 'Sáb', valor: 7400000 },
  { etiqueta: 'Dom', valor: 5100000 },
];

// Platos más vendidos (dona)
export const PLATOS_MAS_VENDIDOS = [
  { etiqueta: 'Pizza Napolitana', valor: 40, color: '#A855F7' },
  { etiqueta: 'Hamburguesa Clásica', valor: 35, color: '#EC4899' },
  { etiqueta: 'Ensalada César', valor: 15, color: '#C084FC' },
  { etiqueta: 'Bebidas', valor: 10, color: '#D946EF' },
];

// Pedidos activos del restaurante. estado: 'nuevo' | 'preparando' | 'listo'
export const PEDIDOS_RESTAURANTE = [
  { id: 'R8920', cliente: 'Carlos Mendoza', estado: 'preparando' },
  { id: 'R8921', cliente: 'Ana López', estado: 'listo' },
  { id: 'R8922', cliente: 'Javier Díaz', estado: 'preparando' },
  { id: 'R8923', cliente: 'Sofía Torres', estado: 'nuevo' },
];

// Gestión del menú (switches)
export const MENU_RESTAURANTE = [
  { id: 1, nombre: 'Pizza Napolitana', activo: true, vendidosHoy: 14 },
  { id: 2, nombre: 'Ensalada César', activo: true, vendidosHoy: 9 },
  { id: 3, nombre: 'Postre del Día', activo: true, vendidosHoy: 5 },
  { id: 4, nombre: 'Menú Infantil', activo: false, vendidosHoy: 0 },
];

// ------------------------------------------------------------
// PANEL DEL CLIENTE
// ------------------------------------------------------------
export const CLIENTE = {
  direccion: 'Calle 12 #34-56, Apto 301, Medellín',
  creditos: 245000,
};

// Pasos del seguimiento de un pedido
export const PASOS_SEGUIMIENTO = ['Confirmado', 'Preparando', 'En camino', 'Entregado'];

// progreso = índice del paso actual dentro de PASOS_SEGUIMIENTO
export const PEDIDOS_CLIENTE = [
  {
    id: 'C8920',
    restaurante: 'Pizzas El Cesar',
    estado: 'En Camino',
    tono: 'morado',
    llegada: '12:40 PM',
    detalle: '1x Pizza Familiar Especial, 2x Gaseosa',
    accion: 'Ver Seguimiento',
    progreso: 2,
  },
  {
    id: 'C8921',
    restaurante: 'Sushi Roll',
    estado: 'Preparando',
    tono: 'ambar',
    llegada: '1:00 PM',
    detalle: 'Combo Sushi Personal',
    accion: 'Ver Detalles',
    progreso: 1,
  },
];

export const FAVORITOS = [
  { id: 1, nombre: 'Pizzas El Cesar', calificacion: 4.9, valoraciones: '540+' },
  { id: 2, nombre: 'Hamburguesas El Corral', calificacion: 4.8, valoraciones: '800+' },
  { id: 3, nombre: 'Pollo Fiesta', calificacion: 4.7, valoraciones: '320+' },
];

export const RECOMENDADOS = [
  { id: 4, nombre: 'La Vaca Fría', categoria: 'Helados', calificacion: 4.9 },
  { id: 5, nombre: 'Taquería El Mariachi', categoria: 'Tacos', calificacion: 4.7 },
];

export const NOTIFICACIONES_CLIENTE = [
  'Tu pedido #C8920 va en camino.',
  'Sushi Roll confirmó tu pedido #C8921.',
  'Nueva promoción: 20% de descuento en sushi.',
];

// ------------------------------------------------------------
// PANEL DEL DOMICILIARIO
// ------------------------------------------------------------
export const DOMICILIARIO = {
  gananciasHoy: 185500,
  entregasHoy: 8,
  calificacion: 4.95,
};

// Entrega que el domiciliario tiene asignada en este momento
export const ENTREGA_ACTIVA = {
  id: 'D8920',
  recoger: { nombre: 'Pizzas El Cesar', direccion: 'Calle 12 #34-56' },
  entregar: { nombre: 'Carlos Mendoza', direccion: 'Cra 50 #12-34, Apto 702' },
};

// Solicitudes cercanas pendientes de aceptar o rechazar
export const ENTREGAS_NUEVAS = [
  { id: 'D8921', origen: 'Hamburguesas El Corral', destino: 'Carrera 70 #5-67', distancia: '2.4 km' },
  { id: 'D8922', origen: 'Sushi Roll', destino: 'Calle 45 #8-90', distancia: '1.8 km' },
];

// Historial rápido del día
export const HISTORIAL_HOY = [
  { id: 1, hora: '1:15 PM', texto: 'Pedido #C8915 (Pollo Fiesta) - Entregado' },
  { id: 2, hora: '12:50 PM', texto: 'Pedido #C8914 (Tacos) - Entregado' },
];
