// ============================================================
// DriverDashboard.jsx — Panel del Domiciliario
// Filas: estado y resumen · entrega activa con mapa · solicitudes + historial
// ============================================================
import { useState } from 'react';
import { Check, CheckCircle2, MapPin, Phone, Star, Store, X } from 'lucide-react';
import Sidebar from '../components/Sidebar.jsx';
import Navbar from '../components/Navbar.jsx';
import { Badge, Card, Toast, useMensajeTemporal } from '../components/Widgets.jsx';
import {
  DOMICILIARIO,
  ENTREGA_ACTIVA,
  ENTREGAS_NUEVAS,
  HISTORIAL_HOY,
  ROLES,
} from '../data/mockData.js';
import { formatCOP } from '../utils/format.js';

// Estados de la entrega activa: pendiente de recoger -> en camino -> entregada
const FLUJO_ENTREGA = {
  pendiente: { etiqueta: 'Por recoger', tono: 'ambar', siguiente: 'Marcar como Recogido' },
  en_camino: { etiqueta: 'En camino', tono: 'morado', siguiente: 'Marcar como Entregado' },
  entregado: { etiqueta: 'Entregado', tono: 'verde', siguiente: 'Entrega completada' },
};

// Mini mapa estilizado (SVG): ruta trazada con pines de restaurante y casa
function MiniMapa() {
  return (
    <svg viewBox="0 0 300 200" className="h-44 w-full rounded-xl border border-line bg-[#0B0B10]" aria-label="Mapa de la ruta">
      <defs>
        <pattern id="cuadricula-mapa" width="30" height="30" patternUnits="userSpaceOnUse">
          <path d="M30 0 L0 0 0 30" fill="none" stroke="#1F1F2A" strokeWidth="1" />
        </pattern>
      </defs>
      <rect width="300" height="200" fill="url(#cuadricula-mapa)" />
      {/* Calles */}
      <path d="M20 40 H130 V160 H270" fill="none" stroke="#272732" strokeWidth="10" />
      <path d="M180 10 V190" fill="none" stroke="#272732" strokeWidth="8" />
      {/* Ruta */}
      <path
        d="M70 60 C 150 60, 160 170, 240 170"
        fill="none"
        stroke="#A855F7"
        strokeWidth="4"
        strokeDasharray="8 6"
        className="drop-shadow-[0_0_6px_rgba(157,0,255,0.9)]"
      />
      {/* Pin del restaurante (morado) */}
      <circle cx="70" cy="60" r="11" fill="#9D00FF" stroke="#FFFFFF" strokeWidth="2" />
      <text x="70" y="64" textAnchor="middle" fill="#FFFFFF" fontSize="11" fontWeight="700">R</text>
      {/* Pin de la casa del cliente (blanco) */}
      <circle cx="240" cy="170" r="11" fill="#FFFFFF" stroke="#A855F7" strokeWidth="2" />
      <text x="240" y="174" textAnchor="middle" fill="#0D0D11" fontSize="11" fontWeight="700">C</text>
    </svg>
  );
}

// Bloque de dirección (recoger / entregar)
function Bloque({ Icono, etiqueta, nombre, direccion }) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 rounded-lg border border-neon/40 bg-neon/10 p-2 text-lilac">
        <Icono size={16} />
      </span>
      <div>
        <p className="text-xs uppercase tracking-wide text-muted">{etiqueta}</p>
        <p className="font-semibold">{nombre}</p>
        <p className="text-sm text-muted">({direccion})</p>
      </div>
    </div>
  );
}

export default function DriverDashboard({ correo, onLogout }) {
  const [seccion, setSeccion] = useState('Inicio');
  const [conectado, setConectado] = useState(true);
  const [estadoEntrega, setEstadoEntrega] = useState('pendiente');
  const [solicitudes, setSolicitudes] = useState(ENTREGAS_NUEVAS);
  const [mensaje, mostrarMensaje] = useMensajeTemporal();

  const flujo = FLUJO_ENTREGA[estadoEntrega];

  // Avanza el estado de la entrega activa al siguiente paso
  function avanzarEntrega() {
    if (estadoEntrega === 'pendiente') {
      setEstadoEntrega('en_camino');
      mostrarMensaje('Pedido recogido. ¡Ruta iniciada!');
    } else if (estadoEntrega === 'en_camino') {
      setEstadoEntrega('entregado');
      mostrarMensaje('Pedido entregado con éxito');
    }
  }

  // Quita una solicitud de la lista (aceptada o rechazada)
  function responderSolicitud(id, aceptada) {
    setSolicitudes((lista) => lista.filter((s) => s.id !== id));
    mostrarMensaje(`Entrega #${id} ${aceptada ? 'aceptada' : 'rechazada'}`);
  }

  return (
    <div className="flex min-h-screen bg-app text-white">
      <Sidebar role={ROLES.DRIVER} activeItem={seccion} onSelect={setSeccion} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar
          titulo="BLINK DELIVERY - Panel del Domiciliario"
          usuario="Domiciliario"
          correo={correo}
          onLogout={onLogout}
        />

        <main className="flex-1 space-y-6 p-6">
          {seccion !== 'Inicio' ? (
            <Card className="py-16 text-center">
              <p className="text-lg font-semibold">{seccion}</p>
              <p className="mt-2 text-sm text-muted">Esta sección está en desarrollo. Vuelve a “Inicio”.</p>
            </Card>
          ) : (
            <>
              {/* SECCIÓN 1: estado y resumen rápido */}
              <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                <Card destacada>
                  <p className="text-sm text-muted">Mi Estado Actual</p>
                  <p className="mt-2 flex items-center gap-2 text-2xl font-bold">
                    <span className={`h-3 w-3 rounded-full ${conectado ? 'bg-ok shadow-[0_0_8px_#22C55E]' : 'bg-err'}`} />
                    {conectado ? 'Conectado' : 'Desconectado'}
                  </p>
                  <button
                    type="button"
                    onClick={() => setConectado((c) => !c)}
                    className="btn-outline mt-4 px-3 py-1.5 text-xs"
                  >
                    {conectado ? 'Ir a Desconectar' : 'Ir a Conectar'}
                  </button>
                </Card>

                <Card>
                  <p className="text-sm text-muted">Ganancias de Hoy (COP)</p>
                  <p className="mt-2 text-2xl font-bold">{formatCOP(DOMICILIARIO.gananciasHoy)} COP</p>
                </Card>

                <Card>
                  <p className="text-sm text-muted">Entregas Completadas Hoy</p>
                  <p className="mt-2 text-2xl font-bold">
                    {DOMICILIARIO.entregasHoy} <span className="text-base font-normal text-muted">Entregas</span>
                  </p>
                </Card>

                <Card>
                  <p className="text-sm text-muted">Calificación del Conductor</p>
                  <p className="mt-2 flex items-center gap-2 text-2xl font-bold">
                    <Star size={22} className="fill-warn text-warn" />
                    {DOMICILIARIO.calificacion}
                  </p>
                </Card>
              </section>

              {/* SECCIÓN 2: entrega activa (info · mapa · botones) */}
              <section>
                <Card destacada>
                  <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                    <h2 className="text-lg font-semibold">Entrega Activa #{ENTREGA_ACTIVA.id}</h2>
                    <Badge tono={flujo.tono}>{flujo.etiqueta}</Badge>
                  </div>

                  <div className="grid grid-cols-1 items-center gap-6 lg:grid-cols-[1fr_1.2fr_auto]">
                    {/* Izquierda: direcciones */}
                    <div className="space-y-5">
                      <Bloque
                        Icono={Store}
                        etiqueta="Recoger en"
                        nombre={ENTREGA_ACTIVA.recoger.nombre}
                        direccion={ENTREGA_ACTIVA.recoger.direccion}
                      />
                      <Bloque
                        Icono={MapPin}
                        etiqueta="Entregar a"
                        nombre={ENTREGA_ACTIVA.entregar.nombre}
                        direccion={ENTREGA_ACTIVA.entregar.direccion}
                      />
                    </div>

                    {/* Centro: mapa */}
                    <MiniMapa />

                    {/* Derecha: acciones */}
                    <div className="flex flex-col gap-3 lg:w-56">
                      <button
                        type="button"
                        onClick={() => mostrarMensaje('Llamando al restaurante...')}
                        className="btn-outline"
                      >
                        <Phone size={16} /> Llamar Restaurante
                      </button>
                      <button
                        type="button"
                        onClick={() => mostrarMensaje('Llamando al cliente...')}
                        className="btn-outline"
                      >
                        <Phone size={16} /> Llamar Cliente
                      </button>
                      <button
                        type="button"
                        onClick={avanzarEntrega}
                        disabled={estadoEntrega === 'entregado'}
                        className="btn-neon py-3 text-sm"
                      >
                        {flujo.siguiente}
                      </button>
                    </div>
                  </div>
                </Card>
              </section>

              {/* SECCIÓN 3: solicitudes nuevas + historial */}
              <section className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Nuevas Entregas Cercanas</h2>
                  {solicitudes.length === 0 && (
                    <p className="py-6 text-center text-sm text-muted">No hay solicitudes pendientes.</p>
                  )}
                  <ul className="divide-y divide-line">
                    {solicitudes.map((s) => (
                      <li key={s.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                        <div>
                          <p className="font-medium">
                            Entrega #{s.id}: {s.origen} <span className="text-muted">→</span> {s.destino}
                          </p>
                          <p className="text-xs text-muted">{s.distancia}</p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => responderSolicitud(s.id, true)}
                            className="inline-flex items-center gap-1 rounded-lg bg-ok px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                          >
                            <Check size={14} /> Aceptar
                          </button>
                          <button
                            type="button"
                            onClick={() => responderSolicitud(s.id, false)}
                            className="inline-flex items-center gap-1 rounded-lg bg-err px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                          >
                            <X size={14} /> Rechazar
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                </Card>

                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Historial Rápido de Hoy</h2>
                  <ul className="divide-y divide-line">
                    {HISTORIAL_HOY.map((h) => (
                      <li key={h.id} className="flex items-center gap-3 py-3 text-sm">
                        <CheckCircle2 size={18} className="shrink-0 text-ok" />
                        <span>
                          <span className="font-semibold">{h.hora}:</span>{' '}
                          <span className="text-muted">{h.texto}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </Card>
              </section>
            </>
          )}
        </main>
      </div>

      <Toast mensaje={mensaje} />
    </div>
  );
}
