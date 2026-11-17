// ============================================================
// CustomerDashboard.jsx — Panel del Cliente (Mis Pedidos)
// Filas: 4 tarjetas de resumen · pedidos activos · favoritos y recomendados
// ============================================================
import { useState } from 'react';
import { Bell, MapPin, Package, Star, Wallet } from 'lucide-react';
import Sidebar from '../components/Sidebar.jsx';
import Navbar from '../components/Navbar.jsx';
import { Badge, Card, Toast, useMensajeTemporal } from '../components/Widgets.jsx';
import {
  CLIENTE,
  FAVORITOS,
  NOTIFICACIONES_CLIENTE,
  PASOS_SEGUIMIENTO,
  PEDIDOS_CLIENTE,
  RECOMENDADOS,
  ROLES,
} from '../data/mockData.js';
import { formatCOP } from '../utils/format.js';

export default function CustomerDashboard({ correo, onLogout }) {
  const [seccion, setSeccion] = useState('Inicio');
  const [direccion, setDireccion] = useState(CLIENTE.direccion);
  const [editandoDireccion, setEditandoDireccion] = useState(false);
  const [creditos, setCreditos] = useState(CLIENTE.creditos);
  const [verNotificaciones, setVerNotificaciones] = useState(false);
  const [pedidoExpandido, setPedidoExpandido] = useState(null); // id del pedido con detalle abierto
  const [mensaje, mostrarMensaje] = useMensajeTemporal();

  // Suma 50.000 COP a los créditos
  function recargarCreditos() {
    setCreditos((c) => c + 50000);
    mostrarMensaje('Recarga de $50.000 COP exitosa');
  }

  // Abre o cierra el detalle/seguimiento de un pedido
  function alternarDetalle(id) {
    setPedidoExpandido((actual) => (actual === id ? null : id));
  }

  return (
    <div className="flex min-h-screen bg-app text-white">
      <Sidebar role={ROLES.CUSTOMER} activeItem={seccion} onSelect={setSeccion} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar titulo="BLINK DELIVERY - Mis Pedidos" usuario="Cliente" correo={correo} onLogout={onLogout} />

        <main className="flex-1 space-y-6 p-6">
          {seccion !== 'Inicio' ? (
            <Card className="py-16 text-center">
              <p className="text-lg font-semibold">{seccion}</p>
              <p className="mt-2 text-sm text-muted">Esta sección está en desarrollo. Vuelve a “Inicio”.</p>
            </Card>
          ) : (
            <>
              {/* SECCIÓN 1: resumen en 4 tarjetas */}
              <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                {/* Dirección principal con edición en línea */}
                <Card>
                  <div className="flex items-center gap-2 text-sm text-muted">
                    <MapPin size={16} className="text-neon" /> Mi Dirección Principal
                  </div>
                  {editandoDireccion ? (
                    <input
                      type="text"
                      value={direccion}
                      onChange={(e) => setDireccion(e.target.value)}
                      aria-label="Dirección principal"
                      className="mt-3 w-full rounded-lg border border-neon bg-app px-3 py-2 text-sm focus:outline-none"
                    />
                  ) : (
                    <p className="mt-3 text-sm font-medium">{direccion}</p>
                  )}
                  <button
                    type="button"
                    onClick={() => setEditandoDireccion((e) => !e)}
                    className="btn-outline mt-4 px-3 py-1.5 text-xs"
                  >
                    {editandoDireccion ? 'Guardar' : 'Editar'}
                  </button>
                </Card>

                {/* Créditos */}
                <Card>
                  <div className="flex items-center gap-2 text-sm text-muted">
                    <Wallet size={16} className="text-neon" /> Mis Créditos (COP)
                  </div>
                  <p className="mt-3 text-2xl font-bold">{formatCOP(creditos)} COP</p>
                  <button type="button" onClick={recargarCreditos} className="btn-neon mt-4 px-3 py-1.5 text-xs">
                    Recargar
                  </button>
                </Card>

                {/* Pedidos en curso */}
                <Card>
                  <div className="flex items-center gap-2 text-sm text-muted">
                    <Package size={16} className="text-neon" /> Pedidos en Curso
                  </div>
                  <p className="mt-3 text-2xl font-bold">{PEDIDOS_CLIENTE.length} Pedidos Activos</p>
                  <p className="mt-4 text-xs text-muted">Sigue cada pedido en la sección inferior.</p>
                </Card>

                {/* Notificaciones: el botón "Ver" despliega la lista */}
                <Card>
                  <div className="flex items-center gap-2 text-sm text-muted">
                    <Bell size={16} className="text-neon" /> Notificaciones
                  </div>
                  <p className="mt-3 text-2xl font-bold">{NOTIFICACIONES_CLIENTE.length} Nuevas</p>
                  <button
                    type="button"
                    onClick={() => setVerNotificaciones((v) => !v)}
                    className="btn-outline mt-4 px-3 py-1.5 text-xs"
                  >
                    {verNotificaciones ? 'Ocultar' : 'Ver'}
                  </button>
                </Card>
              </section>

              {verNotificaciones && (
                <Card>
                  <ul className="list-disc space-y-1 pl-5 text-sm text-muted">
                    {NOTIFICACIONES_CLIENTE.map((texto) => (
                      <li key={texto}>{texto}</li>
                    ))}
                  </ul>
                </Card>
              )}

              {/* SECCIÓN 2: pedidos activos como tarjetas horizontales */}
              <section>
                <h2 className="mb-4 text-lg font-semibold">Pedidos Activos</h2>
                <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                  {PEDIDOS_CLIENTE.map((p) => {
                    const abierto = pedidoExpandido === p.id;
                    return (
                      <Card key={p.id} destacada={p.tono === 'morado'}>
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <p className="font-semibold">
                            Pedido #{p.id} <span className="text-muted">({p.restaurante})</span>
                          </p>
                          <Badge tono={p.tono}>{p.estado}</Badge>
                        </div>
                        <p className="mt-2 text-sm text-muted">
                          Est. Llegada: <span className="font-medium text-white">{p.llegada}</span>
                        </p>
                        <p className="mt-1 text-sm text-muted">
                          Detalle: <span className="text-white">{p.detalle}</span>
                        </p>

                        {/* Seguimiento: barra de pasos */}
                        {abierto && (
                          <div className="mt-4 grid grid-cols-4 gap-2">
                            {PASOS_SEGUIMIENTO.map((paso, i) => (
                              <div key={paso} className="text-center">
                                <div
                                  className={`mx-auto h-2 rounded-full ${
                                    i <= p.progreso ? 'bg-neon-deep shadow-neon' : 'bg-line'
                                  }`}
                                />
                                <p className={`mt-2 text-xs ${i <= p.progreso ? 'text-white' : 'text-muted'}`}>{paso}</p>
                              </div>
                            ))}
                          </div>
                        )}

                        <button
                          type="button"
                          onClick={() => alternarDetalle(p.id)}
                          className="btn-neon mt-4 px-4 py-2 text-xs"
                        >
                          {abierto ? 'Cerrar' : p.accion}
                        </button>
                      </Card>
                    );
                  })}
                </div>
              </section>

              {/* SECCIÓN 3: favoritos (izquierda) y recomendados (derecha) */}
              <section className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Mis Restaurantes Favoritos</h2>
                  <ul className="divide-y divide-line">
                    {FAVORITOS.map((r) => (
                      <li key={r.id} className="flex items-center justify-between gap-3 py-3">
                        <div>
                          <p className="font-medium">{r.nombre}</p>
                          <p className="flex items-center gap-1 text-sm">
                            <Star size={14} className="fill-warn text-warn" />
                            <span className="text-warn">{r.calificacion}</span>
                            <span className="text-muted">- {r.valoraciones} valoraciones</span>
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => mostrarMensaje(`Pedido de ${r.nombre} agregado al carrito`)}
                          className="btn-outline px-3 py-1.5 text-xs"
                        >
                          Pedir de nuevo
                        </button>
                      </li>
                    ))}
                  </ul>
                </Card>

                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Recomendados para ti</h2>
                  <ul className="divide-y divide-line">
                    {RECOMENDADOS.map((r) => (
                      <li key={r.id} className="flex items-center justify-between gap-3 py-3">
                        <div>
                          <p className="font-medium">
                            {r.nombre} <span className="text-sm text-muted">({r.categoria})</span>
                          </p>
                          <p className="flex items-center gap-1 text-sm">
                            <Star size={14} className="fill-warn text-warn" />
                            <span className="text-warn">{r.calificacion}</span>
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => mostrarMensaje(`Abriendo menú de ${r.nombre}`)}
                          className="btn-outline px-3 py-1.5 text-xs"
                        >
                          Ver menú
                        </button>
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
