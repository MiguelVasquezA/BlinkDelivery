// API en producción (Render). Para desarrollo local con el backend en tu máquina, usa "http://localhost:8080/api".
const API_URL = "https://blinkdelivery.onrender.com/api";
const CLAVE_SESION = "blinkdelivery.sesion";

const enc = encodeURIComponent;
const COP = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });

const ESTADOS_PEDIDO = {
  RECIBIDO: { texto: "Recibido", tipo: "aviso" },
  ACEPTADO: { texto: "Aceptado", tipo: "acento" },
  EN_PREPARACION: { texto: "En preparación", tipo: "aviso" },
  LISTO_PARA_ENTREGAR: { texto: "Listo para entregar", tipo: "acento" },
  EN_CAMINO: { texto: "En camino", tipo: "acento" },
  ENTREGADO: { texto: "Entregado", tipo: "ok" },
  CANCELADO: { texto: "Cancelado", tipo: "peligro" },
};
const ESTADOS_DOMICILIARIO = [
  { valor: "DISPONIBLE", etiqueta: "Disponible" },
  { valor: "EN_ENTREGA", etiqueta: "En entrega" },
  { valor: "DESCONECTADO", etiqueta: "Desconectado" },
];
const SI_NO = [
  { valor: "true", etiqueta: "Sí" },
  { valor: "false", etiqueta: "No" },
];

/** Perfiles con los que se entra a cada rol. Las rutas coinciden con los endpoints reales de la API. */
const ROLES = {
  cliente: {
    titulo: "Cliente",
    ruta: "customers",
    clave: "identificacion",
    nombre: (r) => r.nombre,
    detalle: (r) => r.direccion || "",
    crear: [
      { nombre: "identificacion", etiqueta: "Identificación", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre completo", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono (7 a 10 dígitos)", obligatorio: true },
      { nombre: "direccion", etiqueta: "Dirección de entrega", obligatorio: true, completa: true },
      { nombre: "email", etiqueta: "Correo (opcional)", tipo: "email" },
    ],
  },
  restaurante: {
    titulo: "Restaurante",
    ruta: "restaurants",
    clave: "id",
    nombre: (r) => r.nombre,
    detalle: (r) => r.tipoCocina,
    crear: [
      { nombre: "id", etiqueta: "ID del restaurante", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre comercial", obligatorio: true },
      { nombre: "tipoCocina", etiqueta: "Tipo de cocina", obligatorio: true },
      { nombre: "direccion", etiqueta: "Dirección (opcional)" },
      { nombre: "telefono", etiqueta: "Teléfono (opcional)" },
    ],
  },
  domiciliario: {
    titulo: "Domiciliario",
    ruta: "drivers",
    clave: "identificacion",
    nombre: (r) => r.nombre,
    detalle: (r) => r.tipoVehiculo,
    crear: [
      { nombre: "identificacion", etiqueta: "Identificación", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre completo", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono", obligatorio: true },
      { nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo (moto, bici…)", obligatorio: true },
    ],
  },
};

/** Pestañas de cada rol. La función devuelve el HTML de la sección. */
const PESTANAS = {
  cliente: [
    { id: "explorar", icono: "🔎", texto: "Explorar restaurantes", vista: () => vistaExplorar() },
    { id: "menu", icono: "🍽️", texto: "Menú y productos", vista: () => vistaMenu() },
    { id: "pedidos", icono: "📦", texto: "Mis pedidos", vista: () => vistaPedidosCliente() },
    { id: "calificaciones", icono: "⭐", texto: "Calificaciones", vista: () => vistaCalificacionesCliente() },
    { id: "perfil", icono: "👤", texto: "Mi perfil", vista: () => vistaPerfilCliente() },
  ],
  restaurante: [
    { id: "catalogo", icono: "🍽️", texto: "Mi catálogo", vista: () => vistaCatalogo() },
    { id: "categorias", icono: "🏷️", texto: "Categorías", vista: () => vistaCategorias() },
    { id: "pedidos", icono: "📦", texto: "Pedidos recibidos", vista: () => vistaPedidosRestaurante() },
    { id: "datos", icono: "🏪", texto: "Datos del restaurante", vista: () => vistaDatosRestaurante() },
  ],
  domiciliario: [
    { id: "entregas", icono: "🛵", texto: "Entregas", vista: () => vistaEntregas() },
    { id: "calificaciones", icono: "⭐", texto: "Mis calificaciones", vista: () => vistaCalificacionesDomiciliario() },
    { id: "perfil", icono: "👤", texto: "Mi perfil", vista: () => vistaPerfilDomiciliario() },
  ],
};

const estado = { rol: null, perfil: null, pestana: null, filtroRestaurante: "", busqueda: "", categoriaFiltro: "" };
const cache = new Map();
let nombresRestaurantes = new Map();
let tokenRender = 0;
let modalActual = null;

/** Carrito del cliente. Un pedido es de un solo restaurante (RN09): el carrito guarda cuál es. */
const CANTIDAD_MAXIMA = 99;
const carrito = { restauranteId: null, cantidades: new Map() }; // productoId -> unidades
const cantidadesMenu = new Map(); // productoId -> unidades elegidas en la tarjeta (por defecto 1)

// ================================================================ utilidades

const $ = (id) => document.getElementById(id);

function esc(valor) {
  return String(valor ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function moneda(valor) {
  return COP.format(Number(valor) || 0);
}

/**
 * Formatea una fecha/hora del backend. Los pedidos se guardan en hora de Bogotá SIN zona
 * (formato LocalDateTime: 2026-11-23T14:05:09). Colombia no tiene horario de verano, así que
 * se fija el desfase -05:00 para interpretarla igual en cualquier navegador, y se muestra en America/Bogota.
 */
function fecha(texto) {
  if (!texto) return "—";
  const sinZona = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/.test(texto);
  const d = new Date(sinZona ? `${texto.replace(/(\.\d{3})\d+$/, "$1")}-05:00` : texto);
  return Number.isNaN(d.getTime())
    ? esc(texto)
    : d.toLocaleString("es-CO", { timeZone: "America/Bogota", dateStyle: "medium", timeStyle: "short" });
}

function estrellas(valor) {
  const n = Math.min(5, Math.max(0, Math.round(Number(valor) || 0)));
  return `<span class="estrellas" aria-label="${n} de 5">${"★".repeat(n)}${"☆".repeat(5 - n)}</span>`;
}

function insignia(texto, tipo = "") {
  return `<span class="insignia ${tipo}">${esc(texto)}</span>`;
}

function insigniaEstado(codigo) {
  const e = ESTADOS_PEDIDO[codigo] || { texto: codigo || "—", tipo: "" };
  return insignia(e.texto, e.tipo);
}

function insigniaAbierto(abierto) {
  return abierto ? insignia("Abierto", "ok") : insignia("Cerrado", "peligro");
}

function cabecera(titulo, subtitulo, acciones = "") {
  return `<div class="vista-cabecera"><div><h2>${esc(titulo)}</h2><p>${esc(subtitulo)}</p></div>` +
    (acciones ? `<div>${acciones}</div>` : "") + `</div>`;
}

function vacio(icono, titulo, texto) {
  return `<div class="vacio"><span class="vacio-icono">${icono}</span><strong>${esc(titulo)}</strong><span>${esc(texto)}</span></div>`;
}

function rejilla(htmlTarjetas) {
  return `<div class="rejilla">${htmlTarjetas.join("")}</div>`;
}

function tablaHtml(columnas, filas) {
  return `<div class="tabla-contenedor"><table><thead><tr>${columnas.map((c) => `<th>${esc(c)}</th>`).join("")}</tr></thead>` +
    `<tbody>${filas.map((f) => `<tr>${f.map((c) => `<td>${c}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}

function porFechaDesc(a, b) {
  return String(b.fechaHora || b.fecha || "").localeCompare(String(a.fechaHora || a.fecha || ""));
}

function nombreRestaurante(id) {
  return nombresRestaurantes.get(id) || id || "—";
}

async function obtenerRestaurantes() {
  const restaurantes = await api("GET", "/restaurants");
  nombresRestaurantes = new Map(restaurantes.map((r) => [r.id, r.nombre]));
  return restaurantes;
}

function toast(texto, tipo = "ok") {
  const aviso = document.createElement("div");
  aviso.className = `aviso-item ${tipo}`;
  aviso.textContent = texto;
  $("avisos").appendChild(aviso);
  setTimeout(() => aviso.remove(), 4000);
}

// ================================================================ red

async function api(metodo, ruta, cuerpo) {
  const opciones = { method: metodo, headers: {} };
  if (cuerpo !== undefined) {
    opciones.headers["Content-Type"] = "application/json";
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(API_URL + ruta, opciones);
  } catch (err) {
    marcarConexion(false);
    throw new Error("No hay conexión con el servidor. El plan gratuito puede tardar un poco en despertar; intenta de nuevo.");
  }
  marcarConexion(true);

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch (err) {
      datos = null;
    }
  }
  if (!respuesta.ok) {
    throw new Error((datos && datos.error) || `Error ${respuesta.status} al consultar la API.`);
  }
  return datos;
}

/** La conexión se comprueba en silencio: sin indicador visual, solo un aviso en consola si falla. */
function marcarConexion(ok) {
  if (!ok) console.warn("BlinkDelivery: no se pudo contactar la API.");
}

async function comprobarConexion() {
  try {
    await api("GET", "/health");
  } catch (err) {
    marcarConexion(false);
  }
}

// ================================================================ navegación entre pantallas

function mostrarPantalla(id) {
  for (const seccion of document.querySelectorAll(".pantalla")) {
    seccion.classList.toggle("activa", seccion.id === id);
  }
  window.scrollTo(0, 0);
}

// ================================================================ acceso: inicio de sesión y creación de cuenta

let modoAcceso = "login";
let camposAcceso = [];

function mostrarAcceso(rol) {
  estado.rol = rol;
  modoAcceso = "login";
  $("titulo-acceso").textContent = `Acceso ${ROLES[rol].titulo.toLowerCase()}`;
  marcarPestanaAcceso();
  renderFormularioAcceso();
  mostrarPantalla("acceso");
}

function marcarPestanaAcceso() {
  for (const boton of document.querySelectorAll(".pestana-acceso")) {
    boton.classList.toggle("activa", boton.dataset.modo === modoAcceso);
  }
}

function renderFormularioAcceso() {
  const cfg = ROLES[estado.rol];
  const etiquetaId = estado.rol === "restaurante" ? "ID del restaurante" : "Identificación";
  const clave = { nombre: "clave", etiqueta: "Contraseña", tipo: "password", obligatorio: true };

  camposAcceso = modoAcceso === "login"
    ? [{ nombre: "usuario", etiqueta: etiquetaId, obligatorio: true }, clave]
    : [...cfg.crear, { ...clave, etiqueta: "Contraseña (mínimo 6 caracteres)" }];

  const form = $("form-acceso");
  form.className = "formulario-modal formulario-acceso";
  form.innerHTML = camposAcceso.map((c) => campoHtml(c, "")).join("") +
    `<button type="submit" class="boton-primario">${modoAcceso === "login" ? "Ingresar" : "Crear cuenta"}</button>`;

  const ayuda = $("ayuda-acceso");
  ayuda.className = "ayuda";
  ayuda.textContent = modoAcceso === "login"
    ? "Ingresa con tu identificación y tu contraseña."
    : "Si el perfil ya existe y aún no tiene contraseña, se la asignaremos con esta.";
}

async function perfilExiste(ruta, id) {
  try {
    await api("GET", `/${ruta}/${enc(id)}`);
    return true;
  } catch (err) {
    return false;
  }
}

async function enviarAcceso(e) {
  e.preventDefault();
  const form = e.target;
  const boton = form.querySelector('button[type="submit"]');
  const ayuda = $("ayuda-acceso");
  boton.disabled = true;
  try {
    const datos = leerFormulario(form, camposAcceso);
    const rol = estado.rol;
    const cfg = ROLES[rol];

    if (modoAcceso === "login") {
      entrar(rol, await api("POST", "/auth/login", { rol, usuario: datos.usuario, clave: datos.clave }));
      return;
    }

    const id = datos[cfg.clave];
    if (!(await perfilExiste(cfg.ruta, id))) {
      const { clave, ...perfilNuevo } = datos;
      await api("POST", `/${cfg.ruta}`, perfilNuevo);
    }
    await api("POST", "/auth/clave", { rol, usuario: id, clave: datos.clave });
    entrar(rol, await api("POST", "/auth/login", { rol, usuario: id, clave: datos.clave }));
  } catch (err) {
    ayuda.textContent = err.message;
    ayuda.className = "ayuda error";
  } finally {
    boton.disabled = false;
  }
}

/** Vista general de administrador, protegida con una clave antes de mostrarse. */
const ADMIN_CLAVE = "admin123";

function abrirAccesoAdmin() {
  abrirModal({
    titulo: "Acceso de administrador",
    textoEnviar: "Entrar",
    mensajeOk: "",
    campos: [{ nombre: "clave", etiqueta: "Clave de administrador", tipo: "password", obligatorio: true }],
    alTerminar: () => {},
    enviar: async (datos) => {
      if (datos.clave !== ADMIN_CLAVE) throw new Error("Clave de administrador incorrecta.");
      estado.rol = "general";
      estado.perfil = null;
      estado.pestana = null;
      mostrarAplicacion();
    },
  });
}

function entrar(rol, perfil) {
  estado.rol = rol;
  estado.perfil = perfil;
  estado.pestana = PESTANAS[rol][0].id;
  estado.filtroRestaurante = "";
  reiniciarCarrito();
  localStorage.setItem(CLAVE_SESION, JSON.stringify({ rol, clave: perfil[ROLES[rol].clave] }));
  mostrarAplicacion();
}

function mostrarAplicacion() {
  mostrarPantalla("aplicacion");
  $("usuario").textContent = estado.rol === "general" ? "Administrador" : ROLES[estado.rol].nombre(estado.perfil);
  renderNav();
  renderVista();
}

function cambiarRol() {
  localStorage.removeItem(CLAVE_SESION);
  Object.assign(estado, { rol: null, perfil: null, pestana: null, filtroRestaurante: "" });
  reiniciarCarrito();
  mostrarPantalla("inicio");
  comprobarConexion();
}

async function restaurarSesion() {
  let guardada = null;
  try {
    guardada = JSON.parse(localStorage.getItem(CLAVE_SESION) || "null");
  } catch (err) {
    guardada = null;
  }
  if (!guardada || !ROLES[guardada.rol]) return false;
  try {
    const perfil = await api("GET", `/${ROLES[guardada.rol].ruta}/${enc(guardada.clave)}`);
    entrar(guardada.rol, perfil);
    return true;
  } catch (err) {
    return false;
  }
}

function renderNav() {
  const nav = $("navegacion");
  const pestanas = estado.rol && estado.rol !== "general" ? PESTANAS[estado.rol] : [];
  nav.hidden = pestanas.length === 0;
  document.querySelector(".cuerpo").classList.toggle("sin-menu", nav.hidden);
  nav.innerHTML = pestanas
    .map((p) => `<button type="button" class="nav-item ${p.id === estado.pestana ? "activo" : ""}" data-accion="ir-pestana" data-pestana="${p.id}">
        <span class="nav-icono">${p.icono}</span>${esc(p.texto)}</button>`)
    .join("");
}

async function renderVista() {
  const vista = $("vista");
  const token = ++tokenRender;
  vista.innerHTML = '<div class="cargando">Cargando…</div>';

  let funcion;
  if (estado.rol === "general") {
    funcion = () => vistaGeneral();
  } else {
    const pestana = (PESTANAS[estado.rol] || []).find((p) => p.id === estado.pestana);
    funcion = pestana ? pestana.vista : null;
  }
  if (!funcion) return;

  try {
    const html = await funcion();
    if (token === tokenRender) vista.innerHTML = html;
  } catch (err) {
    if (token === tokenRender) {
      vista.innerHTML = vacio("⚠️", "No se pudo cargar esta sección", err.message) +
        `<div><button type="button" class="boton-secundario" data-accion="reintentar">Reintentar</button></div>`;
    }
  }
}

// ================================================================ modal genérico

function campoHtml(c, valor) {
  const req = c.obligatorio ? "required" : "";
  const etiqueta = `${esc(c.etiqueta)}${c.obligatorio ? " *" : ""}`;
  const v = valor === undefined || valor === null ? "" : String(valor);
  let control;

  if (c.tipo === "select" || c.tipo === "bool") {
    let opciones = c.tipo === "bool" ? SI_NO : c.opciones;
    if (!c.obligatorio) opciones = [{ valor: "", etiqueta: "(sin cambio)" }, ...opciones];
    control = `<select name="${c.nombre}" ${req}>${opciones
      .map((o) => `<option value="${esc(o.valor)}"${String(o.valor) === v ? " selected" : ""}>${esc(o.etiqueta)}</option>`)
      .join("")}</select>`;
  } else if (c.tipo === "textarea") {
    control = `<textarea name="${c.nombre}" ${req}>${esc(v)}</textarea>`;
  } else if (c.tipo === "number") {
    const min = c.min !== undefined ? `min="${c.min}"` : "";
    const max = c.max !== undefined ? `max="${c.max}"` : "";
    control = `<input type="number" name="${c.nombre}" value="${esc(v)}" step="any" ${min} ${max} ${req}>`;
  } else {
    const tipoInput = { email: "email", password: "password" }[c.tipo] || "text";
    control = `<input type="${tipoInput}" name="${c.nombre}" value="${esc(v)}" autocomplete="${tipoInput === "password" ? "new-password" : "off"}" ${req}>`;
  }
  return `<label class="${c.completa ? "completa" : ""}"><span>${etiqueta}</span>${control}</label>`;
}

function abrirModal({
  titulo, campos, valores = {}, textoEnviar = "Guardar", enviar,
  alTerminar = () => renderVista(), mensajeOk = "Guardado correctamente.",
}) {
  modalActual = { campos, enviar, alTerminar, mensajeOk };
  $("modal-titulo").textContent = titulo;
  const form = $("modal-form");
  form.innerHTML = campos.map((c) => campoHtml(c, valores[c.nombre])).join("") +
    `<p class="error" hidden></p>
     <div class="acciones">
       <button type="button" class="boton-secundario" data-cerrar-modal>Cancelar</button>
       <button type="submit" class="boton-primario">${esc(textoEnviar)}</button>
     </div>`;
  $("modal").hidden = false;
  const primero = form.querySelector("input, select, textarea");
  if (primero) primero.focus();
}

function cerrarModal() {
  $("modal").hidden = true;
  $("modal-form").innerHTML = "";
  modalActual = null;
}

function leerFormulario(form, campos) {
  const datos = {};
  for (const c of campos) {
    const el = form.elements.namedItem(c.nombre);
    const valor = (el && el.value ? el.value : "").trim();
    if (valor === "") {
      if (c.obligatorio) throw new Error(`Falta completar: ${c.etiqueta}.`);
      continue;
    }
    if (c.tipo === "number") {
      const n = Number(valor);
      if (Number.isNaN(n)) throw new Error(`"${c.etiqueta}" debe ser un número.`);
      datos[c.nombre] = n;
    } else if (c.tipo === "bool") {
      datos[c.nombre] = valor === "true";
    } else {
      datos[c.nombre] = valor;
    }
  }
  return datos;
}

// ================================================================ vistas: cliente

// ---------------------------------------------------------------- buscador y filtros (cliente)

let datosExplorar = { restaurantes: [], categorias: [], categoriasPorRestaurante: new Map() };
let datosMenu = { productos: [], restaurantes: [] };

/** Texto en minúsculas y sin tildes, para comparar búsquedas. */
function normalizar(texto) {
  return String(texto ?? "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").trim();
}

function vistaActual() {
  return estado.pestana === "menu" ? "menu" : "explorar";
}

/** Barra con buscador en tiempo real y, opcionalmente, un desplegable de restaurantes. */
function barraFiltros({ vista, placeholder, restaurantes = null }) {
  const buscador = `
    <div class="buscador">
      <span class="buscador-lupa" aria-hidden="true">🔍</span>
      <input type="search" class="buscador-input" data-buscar="${vista}" value="${esc(estado.busqueda)}"
             placeholder="${esc(placeholder)}" aria-label="${esc(placeholder)}">
      <button type="button" class="buscador-limpiar" data-accion="limpiar-busqueda"
              aria-label="Borrar búsqueda" ${estado.busqueda ? "" : "hidden"}>✕</button>
    </div>`;
  if (!restaurantes) return `<div class="barra-filtros">${buscador}</div>`;

  const opciones = [{ valor: "", etiqueta: "Todos los restaurantes" },
    ...restaurantes.map((r) => ({ valor: r.id, etiqueta: r.nombre }))];
  const actual = opciones.find((o) => o.valor === estado.filtroRestaurante) || opciones[0];
  const desplegable = `
    <div class="dropdown" data-cambio="filtro-restaurante">
      <button type="button" class="dropdown-disparador" aria-haspopup="listbox" aria-expanded="false">
        <span class="dropdown-texto">${esc(actual.etiqueta)}</span><span class="dropdown-flecha" aria-hidden="true">▾</span>
      </button>
      <div class="dropdown-panel" hidden>
        <input type="search" class="dropdown-buscar" placeholder="Buscar restaurante…" aria-label="Buscar restaurante">
        <ul class="dropdown-lista" role="listbox">
          ${opciones.map((o) => `<li role="option" class="dropdown-opcion${o.valor === actual.valor ? " activa" : ""}"
              data-valor="${esc(o.valor)}" aria-selected="${o.valor === actual.valor}">${esc(o.etiqueta)}</li>`).join("")}
        </ul>
      </div>
    </div>`;
  return `<div class="barra-filtros">${desplegable}${buscador}</div>`;
}

function sinResultados(hayDatos, vacioHtml) {
  if (!hayDatos) return vacioHtml;
  return vacio("🔎", "No se encontraron resultados", "Prueba con otras palabras o limpia los filtros.") +
    `<div><button type="button" class="boton-secundario" data-accion="limpiar-filtros">Limpiar filtros</button></div>`;
}

function resultadosExplorarHtml() {
  const q = normalizar(estado.busqueda);
  const visibles = datosExplorar.restaurantes.filter((r) =>
    (!estado.categoriaFiltro || datosExplorar.categoriasPorRestaurante.get(r.id)?.has(estado.categoriaFiltro)) &&
    (!q || normalizar(`${r.nombre} ${r.tipoCocina}`).includes(q)));
  if (!visibles.length) {
    return sinResultados(datosExplorar.restaurantes.length > 0,
      vacio("🏪", "Aún no hay restaurantes", "Cuando se registren restaurantes aparecerán aquí."));
  }
  return rejilla(visibles.map((r) => `
    <article class="tarjeta">
      <h3>${esc(r.nombre)}</h3>
      <p>${esc(r.tipoCocina)}</p>
      <p>Horario: ${esc(r.horaApertura)} – ${esc(r.horaCierre)}</p>
      <div class="pie">${insigniaAbierto(r.abierto)} ${estrellas(r.promedioCalificacion)} <span>${Number(r.promedioCalificacion || 0).toFixed(1)}</span></div>
      <div class="pie"><button type="button" class="boton-secundario" data-accion="ver-menu" data-id="${esc(r.id)}">Ver menú</button></div>
    </article>`));
}

function resultadosMenuHtml() {
  const q = normalizar(estado.busqueda);
  const visibles = datosMenu.productos.filter((p) =>
    (!estado.filtroRestaurante || p.restauranteId === estado.filtroRestaurante) &&
    (!q || normalizar(`${p.nombre} ${p.descripcion}`).includes(q)));
  if (!visibles.length) {
    return sinResultados(datosMenu.productos.length > 0,
      vacio("🍽️", "No hay productos para mostrar", "Cuando los restaurantes publiquen su menú aparecerá aquí."));
  }
  return rejilla(visibles.map(tarjetaProductoHtml));
}

function restauranteAbierto(id) {
  const r = datosMenu.restaurantes.find((x) => x.id === id);
  return Boolean(r && r.abierto);
}

function tarjetaProductoHtml(p) {
  const cantidad = cantidadesMenu.get(p.id) || 1;
  const abierto = restauranteAbierto(p.restauranteId);
  const puedePedir = p.disponible && abierto;
  const motivo = !p.disponible ? "Este plato está agotado." : !abierto ? "El restaurante está cerrado." : "";
  return `
    <article class="tarjeta">
      <div class="pie"><h3>${esc(p.nombre)}</h3>${p.disponible ? insignia("Disponible", "ok") : insignia("Agotado", "peligro")}</div>
      <p>${esc(p.descripcion)}</p>
      <p>${esc(nombreRestaurante(p.restauranteId))}</p>
      <div class="pie"><span class="precio">${moneda(p.precio)}</span></div>
      <div class="pie">
        <div class="selector-cantidad" role="group" aria-label="Cantidad de ${esc(p.nombre)}">
          <button type="button" class="boton-secundario" data-accion="cantidad-tarjeta" data-id="${esc(p.id)}" data-delta="-1"
            aria-label="Quitar una unidad" ${cantidad <= 1 ? "disabled" : ""}>−</button>
          <span aria-live="polite">${cantidad}</span>
          <button type="button" class="boton-secundario" data-accion="cantidad-tarjeta" data-id="${esc(p.id)}" data-delta="1"
            aria-label="Agregar una unidad" ${cantidad >= CANTIDAD_MAXIMA ? "disabled" : ""}>+</button>
        </div>
        <button type="button" class="boton-primario" data-accion="agregar-carrito" data-id="${esc(p.id)}" ${puedePedir ? "" : "disabled"}>Agregar</button>
      </div>
      ${motivo ? `<p class="ayuda">${esc(motivo)}</p>` : ""}
    </article>`;
}

/** Vuelve a pintar solo los resultados, sin recargar datos ni perder el foco del buscador. */
function pintarResultados(vista) {
  const contenedor = $(`resultados-${vista}`);
  if (contenedor) contenedor.innerHTML = vista === "menu" ? resultadosMenuHtml() : resultadosExplorarHtml();
  const limpiar = document.querySelector(".buscador-limpiar");
  if (limpiar) limpiar.hidden = !estado.busqueda;
}

/** Botones de categoría: «Todas» y una por categoría; la activa lleva la clase `active`. */
function chipsCategoriasHtml() {
  const opciones = [{ id: "", nombre: "Todas" }, ...datosExplorar.categorias];
  return opciones.map((c) => {
    const activa = c.id === estado.categoriaFiltro;
    return `<button type="button" class="chip${activa ? " active" : ""}" data-accion="filtrar-categoria"
              data-id="${esc(c.id)}" aria-pressed="${activa}">${esc(c.nombre)}</button>`;
  }).join("");
}

/** Repinta los chips y los resultados sin recargar datos ni perder el texto del buscador. */
function pintarFiltrosExplorar() {
  const chips = $("chips-categorias");
  if (chips) chips.innerHTML = chipsCategoriasHtml();
  pintarResultados("explorar");
}

async function vistaExplorar() {
  const [restaurantes, categorias] = await Promise.all([obtenerRestaurantes(), api("GET", "/categories")]);
  // Relación restaurante-categoría: una petición por restaurante, en paralelo.
  const porRestaurante = await Promise.all(restaurantes.map((r) => api("GET", `/restaurants/${enc(r.id)}/categories`)));
  const categoriasPorRestaurante = new Map(restaurantes.map((r, i) => [r.id, new Set(porRestaurante[i].map((c) => c.id))]));
  datosExplorar = { restaurantes, categorias, categoriasPorRestaurante };
  return cabecera("Explorar restaurantes", "Descubre dónde pedir y revisa sus calificaciones.") +
    (categorias.length ? `<div class="chips" id="chips-categorias">${chipsCategoriasHtml()}</div>` : "") +
    barraFiltros({ vista: "explorar", placeholder: "Buscar restaurante o tipo de cocina…" }) +
    `<div id="resultados-explorar">${resultadosExplorarHtml()}</div>`;
}

async function vistaMenu() {
  const [productos, restaurantes] = await Promise.all([api("GET", "/products"), obtenerRestaurantes()]);
  datosMenu = { productos, restaurantes };
  return cabecera("Menú y productos", "Elige platos de un restaurante y confirma tu pedido.") +
    barraFiltros({ vista: "menu", placeholder: "Buscar plato o descripción…", restaurantes }) +
    `<div class="menu-pedido">
       <div id="resultados-menu">${resultadosMenuHtml()}</div>
       <aside class="carrito" id="carrito" aria-label="Carrito de compras">${carritoHtml()}</aside>
     </div>`;
}

// ---------------------------------------------------------------- carrito (cliente)

function vaciarCarrito() {
  carrito.restauranteId = null;
  carrito.cantidades.clear();
}

/** Vacía el carrito y olvida las cantidades elegidas en las tarjetas (al salir de la sesión). */
function reiniciarCarrito() {
  vaciarCarrito();
  cantidadesMenu.clear();
}

function unidadesTotales() {
  return [...carrito.cantidades.values()].reduce((suma, n) => suma + n, 0);
}

/** Líneas del carrito con los datos del producto. Descarta los que ya no existen en el menú. */
function lineasCarrito() {
  return [...carrito.cantidades.entries()]
    .map(([id, cantidad]) => {
      const producto = datosMenu.productos.find((p) => p.id === id);
      return producto ? { producto, cantidad, subtotal: Number(producto.precio) * cantidad } : null;
    })
    .filter(Boolean);
}

function agregarAlCarrito(id, cantidad) {
  const producto = datosMenu.productos.find((p) => p.id === id);
  if (!producto) throw new Error("Ese plato ya no está disponible en el menú.");
  if (!producto.disponible) throw new Error(`«${producto.nombre}» está agotado.`);
  if (!restauranteAbierto(producto.restauranteId)) {
    throw new Error(`${nombreRestaurante(producto.restauranteId)} está cerrado en este momento.`);
  }
  if (carrito.cantidades.size && carrito.restauranteId !== producto.restauranteId) {
    throw new Error(`Tu pedido es de ${nombreRestaurante(carrito.restauranteId)}. Confírmalo o vacía el carrito para pedir de otro restaurante.`);
  }
  carrito.restauranteId = producto.restauranteId;
  const nueva = Math.min(CANTIDAD_MAXIMA, (carrito.cantidades.get(id) || 0) + cantidad);
  carrito.cantidades.set(id, nueva);
}

function cambiarCantidadCarrito(id, delta) {
  const nueva = (carrito.cantidades.get(id) || 0) + delta;
  if (nueva <= 0) {
    quitarDelCarrito(id);
    return;
  }
  carrito.cantidades.set(id, Math.min(CANTIDAD_MAXIMA, nueva));
}

function quitarDelCarrito(id) {
  carrito.cantidades.delete(id);
  if (!carrito.cantidades.size) carrito.restauranteId = null;
}

function carritoHtml() {
  const lineas = lineasCarrito();
  if (!lineas.length) {
    return `<h3>Tu pedido</h3>
      ${vacio("🛒", "Tu carrito está vacío", "Elige la cantidad de un plato y pulsa «Agregar».")}`;
  }
  const total = lineas.reduce((suma, l) => suma + l.subtotal, 0);
  const abierto = restauranteAbierto(carrito.restauranteId);
  return `
    <div class="carrito-cabecera">
      <h3>Tu pedido</h3>
      <span>${esc(nombreRestaurante(carrito.restauranteId))} · ${unidadesTotales()} ${unidadesTotales() === 1 ? "unidad" : "unidades"}</span>
    </div>
    <ul class="carrito-lineas">
      ${lineas.map((l) => `
        <li>
          <div class="carrito-linea-texto">
            <strong>${esc(l.producto.nombre)}</strong>
            <span>${moneda(l.producto.precio)} c/u</span>
          </div>
          <div class="selector-cantidad" role="group" aria-label="Cantidad de ${esc(l.producto.nombre)}">
            <button type="button" class="boton-secundario" data-accion="cantidad-carrito" data-id="${esc(l.producto.id)}" data-delta="-1" aria-label="Quitar una unidad">−</button>
            <span>${l.cantidad}</span>
            <button type="button" class="boton-secundario" data-accion="cantidad-carrito" data-id="${esc(l.producto.id)}" data-delta="1"
              aria-label="Agregar una unidad" ${l.cantidad >= CANTIDAD_MAXIMA ? "disabled" : ""}>+</button>
          </div>
          <strong class="precio">${moneda(l.subtotal)}</strong>
          <button type="button" class="enlace" data-accion="quitar-carrito" data-id="${esc(l.producto.id)}">Quitar</button>
        </li>`).join("")}
    </ul>
    <div class="carrito-total"><span>Total</span><strong class="precio">${moneda(total)}</strong></div>
    <p class="ayuda">Entrega en: ${esc(estado.perfil.direccion || "—")}</p>
    ${abierto ? "" : `<p class="ayuda error">Este restaurante está cerrado; no se puede confirmar el pedido.</p>`}
    <div class="acciones-carrito">
      <button type="button" class="boton-secundario" data-accion="vaciar-carrito">Vaciar</button>
      <button type="button" class="boton-primario" data-accion="confirmar-pedido" id="btn-confirmar" ${abierto ? "" : "disabled"}>Confirmar pedido</button>
    </div>`;
}

/** Vuelve a pintar el carrito sin recargar el catálogo. */
function pintarCarrito() {
  const el = $("carrito");
  if (el) el.innerHTML = carritoHtml();
}

let confirmandoPedido = false;

async function confirmarPedido() {
  if (confirmandoPedido) return;
  const lineas = lineasCarrito();
  if (!lineas.length) throw new Error("Tu carrito está vacío.");
  confirmandoPedido = true;
  try {
    const pedido = await api("POST", "/orders", {
      clienteId: estado.perfil.identificacion,
      restauranteId: carrito.restauranteId,
      items: lineas.map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
    });
    reiniciarCarrito();
    toast(`Pedido ${pedido.id} creado por ${moneda(pedido.total)}.`);
    estado.pestana = "pedidos";
    estado.busqueda = "";
    renderNav();
    renderVista();
  } finally {
    confirmandoPedido = false;
    pintarCarrito();
  }
}

/**
 * Calificaciones que aún faltan en un pedido entregado (RN12): "RESTAURANTE" y/o "DOMICILIARIO".
 * Cada una se valida por separado; el domiciliario solo aplica si el pedido lo tiene asignado.
 */
function calificacionesPendientes(pedido, calificaciones) {
  const yo = estado.perfil.identificacion;
  if (pedido.clienteId !== yo || pedido.estado !== "ENTREGADO") return [];
  const delPedido = calificaciones.filter((r) => r.clienteId === yo && r.pedidoId === pedido.id);
  const pendientes = [];
  if (!delPedido.some((r) => !r.domiciliarioId)) pendientes.push("RESTAURANTE");
  if (pedido.domiciliarioId && !delPedido.some((r) => r.domiciliarioId === pedido.domiciliarioId)) {
    pendientes.push("DOMICILIARIO");
  }
  return pendientes;
}

/** Pedidos entregados del cliente con al menos una calificación pendiente (RN12). */
function pedidosCalificables(pedidos, calificaciones) {
  return pedidos.filter((o) => calificacionesPendientes(o, calificaciones).length > 0);
}

async function vistaPedidosCliente() {
  const [pedidos, calificaciones] = await Promise.all([api("GET", "/orders"), api("GET", "/ratings"), obtenerRestaurantes()]);
  const yo = estado.perfil.identificacion;
  const mios = pedidos.filter((o) => o.clienteId === yo).sort(porFechaDesc);
  const calificacionesDelCliente = calificaciones.filter((r) => r.clienteId === yo);
  cache.set("elegibles", pedidosCalificables(pedidos, calificaciones));

  return cabecera("Mis pedidos", "Seguimiento de tus pedidos.") +
    (mios.length
      ? rejilla(mios.map((o) => {
          const delPedido = calificacionesDelCliente.filter((r) => r.pedidoId === o.id);
          const deRestaurante = delPedido.find((r) => !r.domiciliarioId);
          const deDomiciliario = delPedido.find((r) => r.domiciliarioId);
          const pie = [
            deRestaurante ? `<span>Restaurante: ${estrellas(deRestaurante.estrellas)}</span>` : "",
            deDomiciliario ? `<span>Domiciliario: ${estrellas(deDomiciliario.estrellas)}</span>` : "",
            calificacionesPendientes(o, calificaciones).length
              ? `<button type="button" class="boton-secundario" data-accion="calificar" data-id="${esc(o.id)}">Calificar</button>`
              : "",
          ].filter(Boolean).join(" ");
          return `
          <article class="tarjeta">
            <div class="pie"><h3>Pedido ${esc(o.id)}</h3>${insigniaEstado(o.estado)}</div>
            <p>${esc(nombreRestaurante(o.restauranteId))}</p>
            <p>${productosHtml(o)}</p>
            <p>Entrega: ${esc(o.direccionEntrega || "—")}</p>
            ${o.domiciliarioNombre ? `<p>Domiciliario: ${esc(o.domiciliarioNombre)}</p>` : ""}
            ${o.guia ? `<p>Guía de seguimiento: ${esc(o.guia)}</p>` : ""}
            <div class="pie"><span>${fecha(o.fechaHora)}</span><strong class="precio">${moneda(o.total)}</strong></div>
            ${pie ? `<div class="pie">${pie}</div>` : ""}
          </article>`;
        }))
      : vacio("📦", "Aún no tienes pedidos", "Elige platos en «Menú y productos» y confirma tu pedido."));
}

async function vistaCalificacionesCliente() {
  const [pedidos, calificaciones] = await Promise.all([api("GET", "/orders"), api("GET", "/ratings"), obtenerRestaurantes()]);
  const yo = estado.perfil.identificacion;
  const mias = calificaciones.filter((r) => r.clienteId === yo).sort(porFechaDesc);
  const elegibles = pedidosCalificables(pedidos, calificaciones);
  cache.set("elegibles", elegibles);

  return cabecera("Calificaciones", "Califica tus pedidos entregados.",
      `<button type="button" class="boton-primario" data-accion="calificar" ${elegibles.length ? "" : "disabled"}>Calificar pedido</button>`) +
    (elegibles.length ? `<p class="ayuda">Tienes ${elegibles.length} pedido(s) pendientes de calificar.</p>` : "") +
    (mias.length
      ? rejilla(mias.map((r) => `
          <article class="tarjeta">
            <div class="pie"><h3>${r.domiciliarioId ? "Domiciliario" : esc(nombreRestaurante(r.restauranteId))}</h3>${estrellas(r.estrellas)}</div>
            <p>${esc(r.comentario || "Sin comentario")}</p>
            <div class="pie"><span>Pedido ${esc(r.pedidoId)}</span><span>${fecha(r.fecha)}</span></div>
          </article>`))
      : vacio("⭐", "Aún no has calificado pedidos", "Cuando un pedido sea entregado podrás calificarlo aquí."));
}

function accionCalificar(pedidoPreseleccionado) {
  const elegibles = cache.get("elegibles") || [];
  if (!elegibles.length) throw new Error("No tienes pedidos entregados por calificar.");
  const opcionesPuntuacion = [
    { valor: "", etiqueta: "No calificar" },
    ...[5, 4, 3, 2, 1].map((n) => ({ valor: String(n), etiqueta: `${"★".repeat(n)}${"☆".repeat(5 - n)} (${n})` })),
  ];
  abrirModal({
    titulo: "Calificar pedido",
    textoEnviar: "Enviar calificación",
    mensajeOk: "Calificación enviada. ¡Gracias!",
    valores: { pedidoId: pedidoPreseleccionado },
    campos: [
      {
        nombre: "pedidoId", etiqueta: "Pedido", tipo: "select", obligatorio: true, completa: true,
        opciones: elegibles.map((o) => ({ valor: o.id, etiqueta: `${o.id} · ${nombreRestaurante(o.restauranteId)}` })),
      },
      { nombre: "estrellasRestaurante", etiqueta: "Puntuación al restaurante", tipo: "select", opciones: opcionesPuntuacion },
      { nombre: "comentarioRestaurante", etiqueta: "Comentario al restaurante (opcional, máximo 200 caracteres)", tipo: "textarea", completa: true },
      { nombre: "estrellasDomiciliario", etiqueta: "Puntuación al domiciliario", tipo: "select", opciones: opcionesPuntuacion },
      { nombre: "comentarioDomiciliario", etiqueta: "Comentario al domiciliario (opcional, máximo 200 caracteres)", tipo: "textarea", completa: true },
    ],
    enviar: async (d) => {
      const pedido = elegibles.find((o) => o.id === d.pedidoId);
      if (!pedido) throw new Error("Ese pedido ya no está disponible para calificar.");
      const pendientes = calificacionesPendientes(pedido, await api("GET", "/ratings"));
      const envios = [];
      if (d.estrellasRestaurante && pendientes.includes("RESTAURANTE")) {
        envios.push({ tipo: "RESTAURANTE", estrellas: d.estrellasRestaurante, comentario: d.comentarioRestaurante });
      }
      if (d.estrellasDomiciliario && pendientes.includes("DOMICILIARIO")) {
        envios.push({ tipo: "DOMICILIARIO", estrellas: d.estrellasDomiciliario, comentario: d.comentarioDomiciliario });
      }
      if (!envios.length) {
        throw new Error("Elige una puntuación para una calificación pendiente de este pedido.");
      }
      // Cada calificación se registra y valida por separado (restaurante y domiciliario).
      for (const e of envios) {
        await api("POST", "/ratings", {
          pedidoId: pedido.id,
          clienteId: estado.perfil.identificacion,
          tipo: e.tipo,
          estrellas: Number(e.estrellas),
          comentario: e.comentario,
        });
      }
      cache.delete("elegibles");
    },
  });
}

async function vistaPerfilCliente() {
  const id = estado.perfil.identificacion;
  const [cliente, direcciones] = await Promise.all([
    api("GET", `/customers/${enc(id)}`),
    api("GET", `/customers/${enc(id)}/addresses`),
  ]);
  estado.perfil = cliente;
  cache.set("direcciones", direcciones);

  return cabecera("Mi perfil", "Tus datos y direcciones de entrega.",
      `<button type="button" class="boton-primario" data-accion="editar-cliente">Editar perfil</button>`) +
    `<article class="tarjeta">
       <h3>${esc(cliente.nombre)}</h3>
       <p>Identificación: ${esc(cliente.identificacion)}</p>
       <p>Teléfono: ${esc(cliente.telefono)}</p>
       <p>Correo: ${esc(cliente.email || "—")}</p>
       <p>Dirección principal: ${esc(cliente.direccion)}</p>
     </article>` +
    `<div class="vista-cabecera"><h3 class="seccion-titulo">Direcciones guardadas</h3>
       <button type="button" class="boton-secundario" data-accion="nueva-direccion">+ Agregar dirección</button></div>` +
    (direcciones.length
      ? rejilla(direcciones.map((d) => `
          <article class="tarjeta">
            <h3>${esc(d.etiqueta)}</h3>
            <p>${esc(d.direccionExacta)}</p>
            <p>${esc([d.barrio, d.ciudad].filter(Boolean).join(" · "))}</p>
            ${d.referencia ? `<p>Referencia: ${esc(d.referencia)}</p>` : ""}
          </article>`))
      : vacio("📍", "Sin direcciones adicionales", "Agrega lugares frecuentes como casa o trabajo."));
}

function accionEditarCliente() {
  const c = estado.perfil;
  abrirModal({
    titulo: "Editar perfil",
    textoEnviar: "Guardar cambios",
    valores: { nombre: c.nombre, telefono: c.telefono, email: c.email, direccion: c.direccion },
    campos: [
      { nombre: "nombre", etiqueta: "Nombre completo", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono", obligatorio: true },
      { nombre: "email", etiqueta: "Correo", tipo: "email" },
      { nombre: "direccion", etiqueta: "Dirección principal", obligatorio: true, completa: true },
    ],
    enviar: async (d) => {
      estado.perfil = await api("PUT", `/customers/${enc(c.identificacion)}`, d);
    },
  });
}

function accionNuevaDireccion() {
  const id = estado.perfil.identificacion;
  abrirModal({
    titulo: "Agregar dirección",
    textoEnviar: "Guardar dirección",
    campos: [
      { nombre: "etiqueta", etiqueta: "Nombre (Casa, Trabajo…)", obligatorio: true },
      { nombre: "direccionExacta", etiqueta: "Dirección exacta", obligatorio: true, completa: true },
      { nombre: "barrio", etiqueta: "Barrio" },
      { nombre: "ciudad", etiqueta: "Ciudad" },
      { nombre: "referencia", etiqueta: "Referencia", completa: true },
    ],
    enviar: async (d) => {
      await api("POST", `/customers/${enc(id)}/addresses`, {
        id: `${id}-DIR-${Date.now().toString(36)}`,
        ...d,
      });
    },
  });
}

// ================================================================ vistas: restaurante

async function vistaCatalogo() {
  const productos = (await api("GET", "/products")).filter((p) => p.restauranteId === estado.perfil.id);
  cache.set("mis-productos", productos);
  return cabecera("Mi catálogo", "Gestiona los productos que ofreces a los clientes.",
      `<button type="button" class="boton-primario" data-accion="nuevo-producto">+ Nuevo producto</button>`) +
    (productos.length
      ? rejilla(productos.map((p) => `
          <article class="tarjeta">
            <div class="pie"><h3>${esc(p.nombre)}</h3>${p.disponible ? insignia("Activo", "ok") : insignia("Inactivo", "peligro")}</div>
            <p>${esc(p.descripcion)}</p>
            <div class="pie"><span class="precio">${moneda(p.precio)}</span></div>
            <div class="pie">
              <button type="button" class="boton-secundario" data-accion="editar-producto" data-id="${esc(p.id)}">Editar</button>
              <button type="button" class="boton-secundario" data-accion="alternar-producto" data-id="${esc(p.id)}" data-valor="${p.disponible}">
                ${p.disponible ? "Desactivar" : "Activar"}</button>
            </div>
          </article>`))
      : vacio("🍽️", "Tu catálogo está vacío", "Agrega tu primer producto para que los clientes puedan verlo."));
}

function accionNuevoProducto() {
  abrirModal({
    titulo: "Nuevo producto",
    textoEnviar: "Crear producto",
    campos: [
      { nombre: "id", etiqueta: "ID del producto", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "descripcion", etiqueta: "Descripción", obligatorio: true, completa: true },
      { nombre: "precio", etiqueta: "Precio (COP)", tipo: "number", min: 0, obligatorio: true },
    ],
    enviar: async (d) => {
      await api("POST", "/products", { ...d, restauranteId: estado.perfil.id });
    },
  });
}

function accionEditarProducto(id) {
  const p = (cache.get("mis-productos") || []).find((x) => x.id === id);
  if (!p) return toast("No se encontró el producto.", "error");
  abrirModal({
    titulo: "Editar producto",
    textoEnviar: "Guardar cambios",
    valores: { nombre: p.nombre, descripcion: p.descripcion, precio: p.precio },
    campos: [
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "descripcion", etiqueta: "Descripción", obligatorio: true, completa: true },
      { nombre: "precio", etiqueta: "Precio (COP)", tipo: "number", min: 0, obligatorio: true },
    ],
    enviar: async (d) => {
      await api("PUT", `/products/${enc(id)}`, d);
    },
  });
}

async function vistaCategorias() {
  const id = estado.perfil.id;
  const [todas, vinculadas] = await Promise.all([api("GET", "/categories"), api("GET", `/restaurants/${enc(id)}/categories`)]);
  cache.set("categorias", todas);
  const ya = new Set(vinculadas.map((c) => c.id));
  const disponibles = todas.filter((c) => !ya.has(c.id));

  return cabecera("Categorías", "Asocia tu restaurante a categorías para que los clientes lo encuentren.",
      `<button type="button" class="boton-primario" data-accion="nueva-categoria">+ Nueva categoría</button>`) +
    `<h3 class="seccion-titulo">Categorías de mi restaurante</h3>` +
    (vinculadas.length
      ? `<div class="chips">${vinculadas.map((c) => `<span class="chip">${esc(c.nombre)}
           <button type="button" class="enlace" data-accion="quitar-categoria" data-id="${esc(c.id)}">quitar</button></span>`).join("")}</div>`
      : vacio("🏷️", "Sin categorías asociadas", "Vincula al menos una categoría para aparecer en las búsquedas.")) +
    (disponibles.length
      ? `<div class="formulario-en-linea">
           <select id="select-categoria">${disponibles.map((c) => `<option value="${esc(c.id)}">${esc(c.nombre)}</option>`).join("")}</select>
           <button type="button" class="boton-primario" data-accion="vincular-categoria">Vincular</button>
         </div>`
      : "") +
    `<h3 class="seccion-titulo">Todas las categorías</h3>` +
    (todas.length
      ? rejilla(todas.map((c) => `<article class="tarjeta"><h3>${esc(c.nombre)}</h3><p>${esc(c.descripcion)}</p></article>`))
      : vacio("🏷️", "No hay categorías todavía", "Crea la primera con el botón de arriba."));
}

function accionNuevaCategoria() {
  abrirModal({
    titulo: "Nueva categoría",
    textoEnviar: "Crear categoría",
    campos: [
      { nombre: "id", etiqueta: "ID (ej. CAT-010)", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "descripcion", etiqueta: "Descripción", obligatorio: true, completa: true },
    ],
    enviar: async (d) => {
      await api("POST", "/categories", d);
    },
  });
}

async function accionVincularCategoria() {
  const select = $("select-categoria");
  if (!select || !select.value) return;
  await api("POST", `/restaurants/${enc(estado.perfil.id)}/categories`, { categoriaId: select.value });
  toast("Categoría vinculada.");
  renderVista();
}

async function accionQuitarCategoria(d) {
  await api("DELETE", `/restaurants/${enc(estado.perfil.id)}/categories/${enc(d.id)}`);
  toast("Categoría quitada.");
  renderVista();
}

/**
 * Lo que el restaurante puede hacer con un pedido: solo avanza hasta LISTO_PARA_ENTREGAR.
 * Después de eso el envío es responsabilidad del domiciliario.
 */
const ACCION_RESTAURANTE = {
  RECIBIDO: { destino: "ACEPTADO", texto: "Aceptar pedido" },
  ACEPTADO: { destino: "EN_PREPARACION", texto: "Empezar preparación" },
  EN_PREPARACION: { destino: "LISTO_PARA_ENTREGAR", texto: "Marcar listo para entregar" },
};

async function vistaPedidosRestaurante() {
  const [pedidos] = await Promise.all([api("GET", "/orders"), obtenerRestaurantes()]);
  const mios = pedidos.filter((o) => o.restauranteId === estado.perfil.id).sort(porFechaDesc);
  return cabecera("Pedidos recibidos", "Pedidos de tu restaurante y su avance.") +
    (mios.length
      ? tablaHtml(["Pedido", "Cliente", "Estado", "Fecha", "Total", "Avanzar"], mios.map((o) => {
          const accion = ACCION_RESTAURANTE[o.estado];
          let celda = "—";
          if (accion) {
            celda = `<button type="button" class="boton-secundario" data-accion="avanzar-pedido" data-id="${esc(o.id)}"
                 data-estado="${accion.destino}">${esc(accion.texto)}</button>`;
          } else if (o.estado === "LISTO_PARA_ENTREGAR") {
            celda = `<span class="ayuda">Esperando domiciliario</span>`;
          }
          return [esc(o.id), esc(o.clienteId), insigniaEstado(o.estado), fecha(o.fechaHora), moneda(o.total), celda];
        }))
      : vacio("📦", "Aún no hay pedidos", "Cuando lleguen pedidos a tu restaurante aparecerán aquí."));
}

async function vistaDatosRestaurante() {
  const r = await api("GET", `/restaurants/${enc(estado.perfil.id)}`);
  estado.perfil = r;
  return cabecera("Datos del restaurante", "Información visible para los clientes.",
      `<button type="button" class="boton-primario" data-accion="editar-restaurante">Editar datos</button>`) +
    `<article class="tarjeta">
       <h3>${esc(r.nombre)}</h3>
       <p>Tipo de cocina: ${esc(r.tipoCocina)}</p>
       <p>Dirección: ${esc(r.direccion)}</p>
       <p>Teléfono: ${esc(r.telefono)}</p>
       <p>Horario: ${esc(r.horaApertura)} – ${esc(r.horaCierre)}</p>
       <div class="pie">${insigniaAbierto(r.abierto)} ${estrellas(r.promedioCalificacion)}
         <span>${Number(r.promedioCalificacion || 0).toFixed(1)}</span></div>
     </article>`;
}

function accionEditarRestaurante() {
  const r = estado.perfil;
  abrirModal({
    titulo: "Editar datos del restaurante",
    textoEnviar: "Guardar cambios",
    valores: { nombre: r.nombre, tipoCocina: r.tipoCocina, direccion: r.direccion, telefono: r.telefono, abierto: String(r.abierto) },
    campos: [
      { nombre: "nombre", etiqueta: "Nombre comercial", obligatorio: true },
      { nombre: "tipoCocina", etiqueta: "Tipo de cocina", obligatorio: true },
      { nombre: "direccion", etiqueta: "Dirección" },
      { nombre: "telefono", etiqueta: "Teléfono" },
      { nombre: "abierto", etiqueta: "Abierto ahora", tipo: "bool" },
    ],
    enviar: async (d) => {
      estado.perfil = await api("PUT", `/restaurants/${enc(r.id)}`, d);
    },
  });
}

// ================================================================ vistas: domiciliario

/** Productos de un pedido en una lista corta para las tarjetas del domiciliario. */
function productosHtml(o) {
  return o.items.map((i) => `${i.cantidad} × ${esc(i.nombre)}`).join("<br>");
}

/** Pedido listo para entregar que aún no tiene domiciliario: el domiciliario puede tomarlo. */
function tarjetaDisponibleHtml(o, puedeTomar) {
  return `
    <article class="tarjeta">
      <div class="pie"><h3>Pedido ${esc(o.id)}</h3>${insigniaEstado(o.estado)}</div>
      <p>Recoger en: ${esc(nombreRestaurante(o.restauranteId))}</p>
      <p>Entregar en: ${esc(o.direccionEntrega || "—")}</p>
      <p>${productosHtml(o)}</p>
      <div class="pie"><span>${fecha(o.fechaHora)}</span><strong class="precio">${moneda(o.total)}</strong></div>
      <div class="pie">
        <button type="button" class="boton-primario" data-accion="tomar-pedido" data-id="${esc(o.id)}"
          ${puedeTomar ? "" : "disabled"}>Tomar pedido</button>
      </div>
    </article>`;
}

/** Pedido asignado a este domiciliario: guía de seguimiento y avance hasta ENTREGADO. */
function tarjetaEntregaHtml(o) {
  const siguiente = o.estado === "EN_CAMINO"
    ? { destino: "ENTREGADO", texto: "Marcar como entregado" }
    : { destino: "EN_CAMINO", texto: "Salir a entrega" };
  return `
    <article class="tarjeta">
      <div class="pie"><h3>Pedido ${esc(o.id)}</h3>${insigniaEstado(o.estado)}</div>
      <p>Recoger en: ${esc(nombreRestaurante(o.restauranteId))}</p>
      <p>Entregar en: ${esc(o.direccionEntrega || "—")}</p>
      <p>${productosHtml(o)}</p>
      <div class="pie"><span>${fecha(o.fechaHora)}</span><strong class="precio">${moneda(o.total)}</strong></div>
      <label class="ayuda" for="guia-${esc(o.id)}">Guía de seguimiento</label>
      <div class="pie">
        <input type="text" id="guia-${esc(o.id)}" value="${esc(o.guia || "")}" maxlength="120"
          placeholder="Número de guía o nota de ubicación">
        <button type="button" class="boton-secundario" data-accion="guardar-guia" data-id="${esc(o.id)}">Actualizar guía</button>
      </div>
      <div class="pie">
        <button type="button" class="boton-primario" data-accion="avanzar-pedido" data-id="${esc(o.id)}"
          data-estado="${siguiente.destino}">${esc(siguiente.texto)}</button>
      </div>
    </article>`;
}

async function vistaEntregas() {
  const yo = estado.perfil.identificacion;
  const [pedidos, driver] = await Promise.all([api("GET", "/orders"), api("GET", `/drivers/${enc(yo)}`), obtenerRestaurantes()]);
  estado.perfil = driver;
  const libres = pedidos.filter((o) => o.estado === "LISTO_PARA_ENTREGAR" && !o.domiciliarioId).sort(porFechaDesc);
  const mias = pedidos
    .filter((o) => o.domiciliarioId === yo && (o.estado === "LISTO_PARA_ENTREGAR" || o.estado === "EN_CAMINO"))
    .sort(porFechaDesc);
  const puedeTomar = driver.estado === "DISPONIBLE" && mias.length === 0;

  return cabecera("Entregas", "Toma pedidos listos para entregar y actualiza su avance.") +
    `<h3>Mi entrega actual</h3>` +
    (mias.length
      ? rejilla(mias.map(tarjetaEntregaHtml))
      : vacio("🛵", "No tienes entregas en curso", "Cuando tomes un pedido aparecerá aquí.")) +
    `<h3>Pedidos disponibles</h3>` +
    (puedeTomar ? "" : `<p class="ayuda">Solo puedes tomar un pedido a la vez y cuando tu estado sea «Disponible» (ahora: ${esc(driver.estado)}).</p>`) +
    (libres.length
      ? rejilla(libres.map((o) => tarjetaDisponibleHtml(o, puedeTomar)))
      : vacio("🛵", "No hay pedidos para entregar", "Cuando un restaurante marque un pedido como listo para entregar aparecerá aquí."));
}

async function vistaCalificacionesDomiciliario() {
  // El servidor calcula promedio (1 decimal), total y comentarios a partir de las calificaciones del domiciliario.
  const driver = await api("GET", `/drivers/${enc(estado.perfil.identificacion)}`);
  estado.perfil = driver;
  const recibidas = driver.comentarios || [];
  return cabecera("Mis calificaciones", "Tu desempeño como domiciliario.") +
    `<div class="metricas">
       <div class="tarjeta-metrica"><div class="etiqueta">Calificación promedio</div>
         <div class="valor">${Number(driver.promedioCalificacion || 0).toFixed(1)} ★</div></div>
       <div class="tarjeta-metrica"><div class="etiqueta">Reseñas</div>
         <div class="valor">${Number(driver.totalCalificaciones || 0)}</div></div>
     </div>` +
    (recibidas.length
      ? rejilla(recibidas.map((r) => `
          <article class="tarjeta">
            <div class="pie"><h3>Pedido ${esc(r.pedidoId)}</h3>${estrellas(r.estrellas)}</div>
            <p>${esc(r.comentario || "Sin comentario")}</p>
            <div class="pie"><span>${fecha(r.fecha)}</span></div>
          </article>`))
      : vacio("⭐", "Aún no tienes calificaciones", "Cuando los clientes califiquen tus entregas aparecerán aquí."));
}

async function vistaPerfilDomiciliario() {
  const d = await api("GET", `/drivers/${enc(estado.perfil.identificacion)}`);
  estado.perfil = d;
  const etiquetaEstado = (ESTADOS_DOMICILIARIO.find((e) => e.valor === d.estado) || { etiqueta: d.estado }).etiqueta;
  return cabecera("Mi perfil", "Tus datos como domiciliario.",
      `<button type="button" class="boton-primario" data-accion="editar-conductor">Editar perfil</button>`) +
    `<article class="tarjeta">
       <h3>${esc(d.nombre)}</h3>
       <p>Identificación: ${esc(d.identificacion)}</p>
       <p>Teléfono: ${esc(d.telefono)}</p>
       <p>Vehículo: ${esc(d.tipoVehiculo)}</p>
       <div class="pie">${insignia(etiquetaEstado, d.estado === "DISPONIBLE" ? "ok" : "aviso")} ${estrellas(d.promedioCalificacion)}
         <span>${Number(d.promedioCalificacion || 0).toFixed(1)} · ${Number(d.totalCalificaciones || 0)} reseñas</span></div>
     </article>`;
}

function accionEditarConductor() {
  const d = estado.perfil;
  abrirModal({
    titulo: "Editar perfil",
    textoEnviar: "Guardar cambios",
    valores: { nombre: d.nombre, telefono: d.telefono, tipoVehiculo: d.tipoVehiculo, estado: d.estado },
    campos: [
      { nombre: "nombre", etiqueta: "Nombre completo", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono", obligatorio: true },
      { nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo", obligatorio: true },
      { nombre: "estado", etiqueta: "Estado", tipo: "select", opciones: ESTADOS_DOMICILIARIO },
    ],
    enviar: async (datos) => {
      estado.perfil = await api("PUT", `/drivers/${enc(d.identificacion)}`, datos);
    },
  });
}

// ================================================================ vista general (administrador)

async function vistaGeneral() {
  const [restaurantes, clientes, domiciliarios, pedidos, productos, calificaciones] = await Promise.all([
    obtenerRestaurantes(),
    api("GET", "/customers"),
    api("GET", "/drivers"),
    api("GET", "/orders"),
    api("GET", "/products"),
    api("GET", "/ratings"),
  ]);

  const abiertos = restaurantes.filter((r) => r.abierto).length;
  const disponibles = domiciliarios.filter((d) => d.estado === "DISPONIBLE").length;
  const productosActivos = productos.filter((p) => p.disponible).length;
  const entregados = pedidos.filter((o) => o.estado === "ENTREGADO");
  const ventas = entregados.reduce((suma, o) => suma + Number(o.total || 0), 0);
  const deRestaurante = calificaciones.filter((r) => !r.domiciliarioId);
  const promedio = deRestaurante.length
    ? deRestaurante.reduce((suma, r) => suma + Number(r.estrellas || 0), 0) / deRestaurante.length
    : null;

  const metricas = [
    ["Restaurantes", restaurantes.length, `${abiertos} abiertos`],
    ["Clientes", clientes.length, "registrados"],
    ["Domiciliarios", domiciliarios.length, `${disponibles} disponibles`],
    ["Pedidos", pedidos.length, `${entregados.length} entregados`],
    ["Productos", productos.length, `${productosActivos} activos`],
    ["Ventas entregadas", moneda(ventas), "suma de pedidos entregados"],
    ["Calificación promedio", promedio === null ? "—" : `${promedio.toFixed(1)} ★`, `${deRestaurante.length} calificaciones`],
  ];

  const recientes = [...pedidos].sort(porFechaDesc).slice(0, 10);

  return cabecera("Vista general", "Resumen con datos en vivo de la API.") +
    `<div class="metricas">${metricas.map(([etiqueta, valor, nota]) => `
        <div class="tarjeta-metrica"><div class="etiqueta">${esc(etiqueta)}</div>
          <div class="valor">${esc(valor)}</div><div class="etiqueta">${esc(nota)}</div></div>`).join("")}
     </div>` +
    `<h3 class="seccion-titulo">Pedidos recientes</h3>` +
    (recientes.length
      ? tablaHtml(["Pedido", "Restaurante", "Cliente", "Estado", "Fecha", "Total"], recientes.map((o) => [
          esc(o.id), esc(nombreRestaurante(o.restauranteId)), esc(o.clienteId), insigniaEstado(o.estado),
          fecha(o.fechaHora), moneda(o.total),
        ]))
      : vacio("📦", "Aún no hay pedidos", "Los pedidos aparecerán aquí cuando se registren."));
}

// ================================================================ acciones (delegación de eventos)

// ---------------------------------------------------------------- desplegable personalizado

function cerrarDropdowns() {
  for (const drop of document.querySelectorAll(".dropdown")) {
    drop.querySelector(".dropdown-panel").hidden = true;
    drop.querySelector(".dropdown-disparador").setAttribute("aria-expanded", "false");
    drop.classList.remove("abierto");
  }
}

function alternarDropdown(drop) {
  const panel = drop.querySelector(".dropdown-panel");
  const abrir = panel.hidden;
  cerrarDropdowns();
  if (!abrir) return;
  panel.hidden = false;
  drop.classList.add("abierto");
  drop.querySelector(".dropdown-disparador").setAttribute("aria-expanded", "true");
  drop.querySelector(".dropdown-buscar").focus();
}

function seleccionarDropdown(drop, valor) {
  cerrarDropdowns();
  if (drop.dataset.cambio !== "filtro-restaurante") return;
  const opciones = [...drop.querySelectorAll(".dropdown-opcion")];
  const elegida = opciones.find((li) => li.dataset.valor === valor) || opciones[0];
  for (const li of opciones) {
    const activa = li === elegida;
    li.classList.toggle("activa", activa);
    li.setAttribute("aria-selected", String(activa));
  }
  drop.querySelector(".dropdown-texto").textContent = elegida.textContent;
  estado.filtroRestaurante = elegida.dataset.valor;
  pintarResultados("menu");
}

function filtrarOpcionesDropdown(input) {
  const q = normalizar(input.value);
  for (const li of input.closest(".dropdown").querySelectorAll(".dropdown-opcion")) {
    li.hidden = !normalizar(li.textContent).includes(q);
  }
}

const ACCIONES = {
  "ir-pestana": (d) => {
    estado.pestana = d.pestana;
    estado.busqueda = "";
    renderNav();
    renderVista();
  },
  "ver-menu": (d) => {
    estado.filtroRestaurante = d.id;
    estado.busqueda = "";
    estado.pestana = "menu";
    renderNav();
    renderVista();
  },
  "limpiar-busqueda": () => {
    estado.busqueda = "";
    const input = document.querySelector(".buscador-input");
    if (input) input.value = "";
    pintarResultados(vistaActual());
  },
  "limpiar-filtros": () => {
    estado.busqueda = "";
    estado.filtroRestaurante = "";
    estado.categoriaFiltro = "";
    renderVista();
  },
  // Pulsar la categoría activa la quita (vuelve a «Todas»).
  "filtrar-categoria": (d) => {
    estado.categoriaFiltro = d.id === estado.categoriaFiltro ? "" : d.id;
    pintarFiltrosExplorar();
  },
  reintentar: () => renderVista(),
  "cantidad-tarjeta": (d) => {
    const id = d.id;
    const nueva = Math.min(CANTIDAD_MAXIMA, Math.max(1, (cantidadesMenu.get(id) || 1) + Number(d.delta)));
    cantidadesMenu.set(id, nueva);
    pintarResultados("menu");
  },
  "agregar-carrito": (d) => {
    const cantidad = cantidadesMenu.get(d.id) || 1;
    agregarAlCarrito(d.id, cantidad);
    pintarCarrito();
    toast(`Agregado al pedido: ${cantidad} × ${datosMenu.productos.find((p) => p.id === d.id)?.nombre ?? "plato"}.`);
  },
  "cantidad-carrito": (d) => {
    cambiarCantidadCarrito(d.id, Number(d.delta));
    pintarCarrito();
  },
  "quitar-carrito": (d) => {
    quitarDelCarrito(d.id);
    pintarCarrito();
  },
  "vaciar-carrito": () => {
    vaciarCarrito();
    pintarCarrito();
  },
  "confirmar-pedido": () => confirmarPedido(),
  "avanzar-pedido": async (d) => {
    await api("PUT", `/orders/${enc(d.id)}/estado`, { estado: d.estado });
    toast(`Pedido ${d.id}: ${ESTADOS_PEDIDO[d.estado].texto}.`);
    renderVista();
  },
  "tomar-pedido": async (d) => {
    await api("PUT", `/orders/${enc(d.id)}/domiciliario`, { domiciliarioId: estado.perfil.identificacion });
    toast(`Tomaste el pedido ${d.id}.`);
    renderVista();
  },
  "guardar-guia": async (d) => {
    const campo = $(`guia-${d.id}`);
    await api("PUT", `/orders/${enc(d.id)}/guia`, { guia: campo ? campo.value.trim() : "" });
    toast("Guía actualizada.");
    renderVista();
  },
  calificar: (d) => accionCalificar(d.id),
  "editar-cliente": () => accionEditarCliente(),
  "nueva-direccion": () => accionNuevaDireccion(),
  "nuevo-producto": () => accionNuevoProducto(),
  "editar-producto": (d) => accionEditarProducto(d.id),
  "alternar-producto": async (d) => {
    await api("PUT", `/products/${enc(d.id)}`, { disponible: d.valor !== "true" });
    toast("Producto actualizado.");
    renderVista();
  },
  "nueva-categoria": () => accionNuevaCategoria(),
  "vincular-categoria": () => accionVincularCategoria(),
  "quitar-categoria": (d) => accionQuitarCategoria(d),
  "editar-restaurante": () => accionEditarRestaurante(),
  "editar-conductor": () => accionEditarConductor(),
};

document.addEventListener("click", (e) => {
  const opcion = e.target.closest(".dropdown-opcion");
  if (opcion) {
    seleccionarDropdown(opcion.closest(".dropdown"), opcion.dataset.valor);
    return;
  }
  const disparador = e.target.closest(".dropdown-disparador");
  if (disparador) {
    alternarDropdown(disparador.closest(".dropdown"));
    return;
  }
  if (!e.target.closest(".dropdown")) cerrarDropdowns();

  const cerrar = e.target.closest("[data-cerrar-modal]");
  if (cerrar || e.target.id === "modal-cerrar" || e.target === $("modal")) {
    cerrarModal();
    return;
  }

  const volver = e.target.closest("[data-volver]");
  if (volver) {
    mostrarPantalla(volver.dataset.volver);
    return;
  }

  const boton = e.target.closest("[data-accion]");
  if (!boton || boton.disabled) return;
  const accion = ACCIONES[boton.dataset.accion];
  if (accion) {
    Promise.resolve()
      .then(() => accion(boton.dataset, boton))
      .catch((err) => toast(err.message, "error"));
  }
});

// Búsqueda en tiempo real: filtra las tarjetas sin recargar datos.
document.addEventListener("input", (e) => {
  const t = e.target;
  if (t.dataset && t.dataset.buscar) {
    estado.busqueda = t.value;
    pintarResultados(t.dataset.buscar);
  } else if (t.classList.contains("dropdown-buscar")) {
    filtrarOpcionesDropdown(t);
  }
});

document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;
  if (!$("modal").hidden) cerrarModal();
  cerrarDropdowns();
});

$("modal-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!modalActual) return;
  const form = e.target;
  const errorEl = form.querySelector(".error");
  const boton = form.querySelector('button[type="submit"]');
  errorEl.hidden = true;
  boton.disabled = true;
  try {
    const datos = leerFormulario(form, modalActual.campos);
    const { enviar, alTerminar, mensajeOk } = modalActual;
    await enviar(datos);
    cerrarModal();
    if (mensajeOk) toast(mensajeOk);
    await alTerminar();
  } catch (err) {
    errorEl.textContent = err.message;
    errorEl.hidden = false;
  } finally {
    boton.disabled = false;
  }
});

// ================================================================ resplandor del cursor

/**
 * Resplandor morado que sigue al cursor con interpolación (lerp) en requestAnimationFrame.
 * Solo responde al mouse: en táctil o sin movimiento queda el resplandor ambiental (.reposo).
 * Las tarjetas de rol se iluminan según la cercanía del cursor (variable --prox en CSS).
 */
function iniciarResplandor() {
  const halo = $("resplandor-halo");
  const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const suavizado = 0.1;
  let objetivo = { x: window.innerWidth / 2, y: window.innerHeight * 0.4 };
  let actual = { ...objetivo };
  let bucleActivo = false;

  const pintar = () => {
    halo.style.transform = `translate3d(${actual.x}px, ${actual.y}px, 0) translate(-50%, -50%)`;
  };

  const actualizarTarjetas = (x, y) => {
    for (const tarjeta of document.querySelectorAll(".tarjeta-rol")) {
      const r = tarjeta.getBoundingClientRect();
      const dx = Math.max(r.left - x, 0, x - r.right);
      const dy = Math.max(r.top - y, 0, y - r.bottom);
      const prox = Math.max(0, 1 - Math.hypot(dx, dy) / 180);
      tarjeta.style.setProperty("--prox", prox.toFixed(3));
      tarjeta.style.setProperty("--mx", `${x - r.left}px`);
      tarjeta.style.setProperty("--my", `${y - r.top}px`);
    }
  };

  const tick = () => {
    actual.x += (objetivo.x - actual.x) * suavizado;
    actual.y += (objetivo.y - actual.y) * suavizado;
    pintar();
    actualizarTarjetas(objetivo.x, objetivo.y);
    if (Math.abs(objetivo.x - actual.x) > 0.5 || Math.abs(objetivo.y - actual.y) > 0.5) {
      requestAnimationFrame(tick);
    } else {
      bucleActivo = false;
    }
  };

  window.addEventListener("pointermove", (e) => {
    if (e.pointerType !== "mouse") return;
    halo.classList.remove("reposo");
    objetivo = { x: e.clientX, y: e.clientY };
    if (reducirMovimiento) {
      actual = { ...objetivo };
      pintar();
      actualizarTarjetas(objetivo.x, objetivo.y);
    } else if (!bucleActivo) {
      bucleActivo = true;
      requestAnimationFrame(tick);
    }
  }, { passive: true });

  // Al salir de la ventana, las tarjetas dejan de iluminarse.
  document.documentElement.addEventListener("mouseleave", () => {
    for (const tarjeta of document.querySelectorAll(".tarjeta-rol")) {
      tarjeta.style.setProperty("--prox", "0");
    }
  });

  halo.classList.add("reposo");
  pintar();
}

// ================================================================ arranque

function iniciar() {
  iniciarResplandor();
  document.querySelectorAll("[data-rol]").forEach((boton) => {
    boton.addEventListener("click", () => mostrarAcceso(boton.dataset.rol));
  });
  document.querySelectorAll(".pestana-acceso").forEach((boton) => {
    boton.addEventListener("click", () => {
      modoAcceso = boton.dataset.modo;
      marcarPestanaAcceso();
      renderFormularioAcceso();
    });
  });
  $("form-acceso").addEventListener("submit", enviarAcceso);
  $("btn-admin").addEventListener("click", abrirAccesoAdmin);
  $("btn-cambiar").addEventListener("click", cambiarRol);

  comprobarConexion();
  restaurarSesion().then((restaurada) => {
    if (!restaurada) mostrarPantalla("inicio");
  });
}

iniciar();
