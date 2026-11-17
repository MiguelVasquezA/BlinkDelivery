// API en producción (Render). Para desarrollo local con el backend en tu máquina, usa "http://localhost:8080/api".
const API_URL = "https://blinkdelivery.onrender.com/api";

/**
 * Configuración de cada recurso de la API.
 * columnas: [clave JSON, etiqueta]. campos: formulario de creación (tipo text | number | bool).
 * Si no hay campos, el recurso es de solo lectura en esta interfaz.
 */
const RECURSOS = {
  clientes: {
    titulo: "Clientes",
    ruta: "customers",
    columnas: [
      ["identificacion", "Identificación"], ["nombre", "Nombre"], ["telefono", "Teléfono"],
      ["email", "Correo"], ["direccion", "Dirección"],
    ],
    campos: [
      { nombre: "identificacion", etiqueta: "Identificación", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono (7 a 10 dígitos)", obligatorio: true },
      { nombre: "direccion", etiqueta: "Dirección", obligatorio: true },
      { nombre: "email", etiqueta: "Correo (opcional)" },
    ],
  },
  restaurantes: {
    titulo: "Restaurantes",
    ruta: "restaurants",
    columnas: [
      ["id", "ID"], ["nombre", "Nombre"], ["tipoCocina", "Tipo de cocina"],
      ["direccion", "Dirección"], ["telefono", "Teléfono"], ["abierto", "Abierto"],
    ],
    campos: [
      { nombre: "id", etiqueta: "ID", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "tipoCocina", etiqueta: "Tipo de cocina", obligatorio: true },
      { nombre: "direccion", etiqueta: "Dirección (opcional)" },
      { nombre: "telefono", etiqueta: "Teléfono (opcional)" },
      { nombre: "abierto", etiqueta: "Abierto", tipo: "bool" },
    ],
  },
  productos: {
    titulo: "Productos",
    ruta: "products",
    columnas: [
      ["id", "ID"], ["restauranteId", "Restaurante"], ["nombre", "Nombre"],
      ["descripcion", "Descripción"], ["precio", "Precio"], ["disponible", "Disponible"],
    ],
    campos: [
      { nombre: "id", etiqueta: "ID", obligatorio: true },
      { nombre: "restauranteId", etiqueta: "ID del restaurante", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "descripcion", etiqueta: "Descripción", obligatorio: true },
      { nombre: "precio", etiqueta: "Precio", tipo: "number", obligatorio: true, min: 0 },
      { nombre: "disponible", etiqueta: "Disponible", tipo: "bool" },
    ],
  },
  categorias: {
    titulo: "Categorías",
    ruta: "categories",
    columnas: [["id", "ID"], ["nombre", "Nombre"], ["descripcion", "Descripción"]],
    campos: [
      { nombre: "id", etiqueta: "ID", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "descripcion", etiqueta: "Descripción", obligatorio: true },
    ],
  },
  domiciliarios: {
    titulo: "Domiciliarios",
    ruta: "drivers",
    columnas: [
      ["identificacion", "Identificación"], ["nombre", "Nombre"], ["telefono", "Teléfono"],
      ["tipoVehiculo", "Vehículo"], ["estado", "Estado"], ["calificacionPromedio", "Calificación"],
    ],
    campos: [
      { nombre: "identificacion", etiqueta: "Identificación", obligatorio: true },
      { nombre: "nombre", etiqueta: "Nombre", obligatorio: true },
      { nombre: "telefono", etiqueta: "Teléfono", obligatorio: true },
      { nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo", obligatorio: true },
    ],
  },
  pedidos: {
    titulo: "Pedidos (solo lectura)",
    ruta: "orders",
    columnas: [
      ["id", "ID"], ["clienteId", "Cliente"], ["restauranteId", "Restaurante"],
      ["estado", "Estado"], ["fechaHora", "Fecha"], ["total", "Total"],
    ],
    campos: [],
  },
  calificaciones: {
    titulo: "Calificaciones",
    ruta: "ratings",
    columnas: [
      ["id", "ID"], ["pedidoId", "Pedido"], ["clienteId", "Cliente"], ["restauranteId", "Restaurante"],
      ["estrellas", "Estrellas"], ["comentario", "Comentario"], ["fecha", "Fecha"],
    ],
    campos: [
      { nombre: "id", etiqueta: "ID", obligatorio: true },
      { nombre: "pedidoId", etiqueta: "ID del pedido", obligatorio: true },
      { nombre: "clienteId", etiqueta: "ID del cliente", obligatorio: true },
      { nombre: "restauranteId", etiqueta: "ID del restaurante", obligatorio: true },
      { nombre: "estrellas", etiqueta: "Estrellas (1 a 5)", tipo: "number", obligatorio: true, min: 1, max: 5 },
      { nombre: "comentario", etiqueta: "Comentario (opcional)" },
    ],
  },
};

let recursoActual = "clientes";

const el = {
  estadoApi: document.getElementById("estado-api"),
  menu: document.getElementById("menu"),
  titulo: document.getElementById("titulo-recurso"),
  recargar: document.getElementById("btn-recargar"),
  mensaje: document.getElementById("mensaje"),
  tabla: document.getElementById("tabla"),
  tarjetaFormulario: document.getElementById("tarjeta-formulario"),
  formulario: document.getElementById("formulario"),
};

// ------------------------------------------------------------------ red

/** Llama a la API y devuelve el JSON. Lanza Error con el mensaje del servidor si falla. */
async function llamarApi(metodo, ruta, cuerpo) {
  const opciones = { method: metodo, headers: {} };
  if (cuerpo !== undefined) {
    opciones.headers["Content-Type"] = "application/json";
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, opciones);
  } catch (err) {
    throw new Error(`No se pudo conectar con ${API_URL}. ¿Está el servidor encendido?`);
  }

  const texto = await respuesta.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!respuesta.ok) {
    const detalle = datos && datos.error ? datos.error : `Error HTTP ${respuesta.status}`;
    throw new Error(`${respuesta.status}: ${detalle}`);
  }
  return datos;
}

async function comprobarApi() {
  try {
    await llamarApi("GET", "/health");
    el.estadoApi.textContent = "API conectada";
    el.estadoApi.className = "estado conectado";
  } catch (err) {
    el.estadoApi.textContent = "API sin conexión";
    el.estadoApi.className = "estado desconectado";
  }
}

// ------------------------------------------------------------------ interfaz

function mostrarMensaje(texto, tipo) {
  el.mensaje.textContent = texto;
  el.mensaje.className = `mensaje ${tipo}`;
  el.mensaje.hidden = false;
}

function ocultarMensaje() {
  el.mensaje.hidden = true;
}

function formatear(valor) {
  if (valor === null || valor === undefined) return "—";
  if (typeof valor === "boolean") return valor ? "Sí" : "No";
  if (typeof valor === "object") return JSON.stringify(valor);
  return String(valor);
}

function construirMenu() {
  el.menu.innerHTML = "";
  for (const [clave, recurso] of Object.entries(RECURSOS)) {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.textContent = recurso.titulo;
    boton.dataset.recurso = clave;
    boton.addEventListener("click", () => seleccionar(clave));
    el.menu.appendChild(boton);
  }
}

function marcarMenu() {
  for (const boton of el.menu.querySelectorAll("button")) {
    boton.classList.toggle("activo", boton.dataset.recurso === recursoActual);
  }
}

function renderTabla(filas, columnas) {
  el.tabla.innerHTML = "";
  const tabla = document.createElement("table");

  const thead = tabla.createTHead();
  const encabezado = thead.insertRow();
  for (const [, etiqueta] of columnas) {
    const th = document.createElement("th");
    th.textContent = etiqueta;
    encabezado.appendChild(th);
  }

  const tbody = tabla.createTBody();
  if (filas.length === 0) {
    const fila = tbody.insertRow();
    const td = fila.insertCell();
    td.colSpan = columnas.length;
    td.className = "vacio";
    td.textContent = "Sin registros.";
  }
  for (const registro of filas) {
    const fila = tbody.insertRow();
    for (const [clave] of columnas) {
      fila.insertCell().textContent = formatear(registro[clave]);
    }
  }
  el.tabla.appendChild(tabla);
}

async function cargarTabla() {
  const recurso = RECURSOS[recursoActual];
  ocultarMensaje();
  try {
    const filas = await llamarApi("GET", `/${recurso.ruta}`);
    renderTabla(filas, recurso.columnas);
  } catch (err) {
    el.tabla.innerHTML = "";
    mostrarMensaje(err.message, "error");
  }
}

function renderFormulario() {
  const recurso = RECURSOS[recursoActual];
  el.formulario.innerHTML = "";

  if (recurso.campos.length === 0) {
    el.tarjetaFormulario.hidden = true;
    return;
  }
  el.tarjetaFormulario.hidden = false;

  for (const campo of recurso.campos) {
    const etiqueta = document.createElement("label");
    const titulo = document.createElement("span");
    titulo.textContent = campo.etiqueta + (campo.obligatorio ? " *" : "");
    etiqueta.appendChild(titulo);

    let input;
    if (campo.tipo === "bool") {
      input = document.createElement("select");
      input.add(new Option("(por defecto)", ""));
      input.add(new Option("Sí", "true"));
      input.add(new Option("No", "false"));
    } else {
      input = document.createElement("input");
      input.type = campo.tipo === "number" ? "number" : "text";
      if (campo.tipo === "number") {
        input.step = "any";
        if (campo.min !== undefined) input.min = campo.min;
        if (campo.max !== undefined) input.max = campo.max;
      }
    }
    input.name = campo.nombre;
    input.required = Boolean(campo.obligatorio);
    etiqueta.appendChild(input);
    el.formulario.appendChild(etiqueta);
  }

  const acciones = document.createElement("div");
  acciones.className = "acciones";
  const enviar = document.createElement("button");
  enviar.type = "submit";
  enviar.textContent = "Crear";
  acciones.appendChild(enviar);
  el.formulario.appendChild(acciones);
}

/** Convierte los valores del formulario al cuerpo JSON. Omite los campos opcionales vacíos. */
function leerFormulario() {
  const recurso = RECURSOS[recursoActual];
  const cuerpo = {};
  for (const campo of recurso.campos) {
    const valor = el.formulario.elements[campo.nombre].value.trim();
    if (valor === "") {
      if (campo.obligatorio) throw new Error(`Falta el campo "${campo.etiqueta}".`);
      continue;
    }
    if (campo.tipo === "number") {
      cuerpo[campo.nombre] = Number(valor);
    } else if (campo.tipo === "bool") {
      cuerpo[campo.nombre] = valor === "true";
    } else {
      cuerpo[campo.nombre] = valor;
    }
  }
  return cuerpo;
}

async function enviarFormulario(evento) {
  evento.preventDefault();
  const boton = el.formulario.querySelector('button[type="submit"]');
  boton.disabled = true;
  try {
    const cuerpo = leerFormulario();
    const creado = await llamarApi("POST", `/${RECURSOS[recursoActual].ruta}`, cuerpo);
    mostrarMensaje(`Registro creado: ${JSON.stringify(creado)}`, "ok");
    el.formulario.reset();
    await cargarTabla();
  } catch (err) {
    mostrarMensaje(err.message, "error");
  } finally {
    boton.disabled = false;
  }
}

function seleccionar(clave) {
  recursoActual = clave;
  el.titulo.textContent = RECURSOS[clave].titulo;
  marcarMenu();
  renderFormulario();
  cargarTabla();
}

// ------------------------------------------------------------------ arranque

el.formulario.addEventListener("submit", enviarFormulario);
el.recargar.addEventListener("click", cargarTabla);

construirMenu();
seleccionar(recursoActual);
comprobarApi();
