// ============================================================
// AdminDashboard.jsx — Panel de Administración
// Filas: 4 KPI · gráfico de ventas + dona · conductores + restaurantes
// ============================================================
import { useState } from 'react';
import Sidebar from '../components/Sidebar.jsx';
import Navbar from '../components/Navbar.jsx';
import {
  Avatar,
  Badge,
  Card,
  DonutChart,
  KpiCard,
  LineChart,
  SeccionEnDesarrollo,
  SelectorFecha,
  Toggle,
} from '../components/Widgets.jsx';
import {
  CATEGORIAS_PEDIDOS,
  CONDUCTORES_ACTIVOS,
  METRICAS_ADMIN,
  RESTAURANTES_ADMIN,
  ROLES,
  VENTAS_DIA_ADMIN,
  VENTAS_SEMANA_ADMIN,
} from '../data/mockData.js';
import { formatCOP, formatNumber } from '../utils/format.js';

export default function AdminDashboard({ correo, onLogout }) {
  // Ítem activo del sidebar (navegación interna en estado local)
  const [seccion, setSeccion] = useState('Inicio');
  // Periodo del gráfico de ventas: 'Diario' o 'Semanal'
  const [periodo, setPeriodo] = useState('Semanal');
  // Lista de restaurantes con su estado activo/inactivo
  const [restaurantes, setRestaurantes] = useState(RESTAURANTES_ADMIN);

  const m = METRICAS_ADMIN;
  const esSemanal = periodo === 'Semanal';
  const datosVentas = esSemanal ? VENTAS_SEMANA_ADMIN : VENTAS_DIA_ADMIN;

  // Invierte el estado de un restaurante (switch)
  function alternarRestaurante(id) {
    setRestaurantes((lista) => lista.map((r) => (r.id === id ? { ...r, activo: !r.activo } : r)));
  }

  return (
    <div className="flex min-h-screen bg-app text-white">
      <Sidebar role={ROLES.ADMIN} activeItem={seccion} onSelect={setSeccion} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar titulo="Panel de Administración" buscador usuario="admin" correo={correo} onLogout={onLogout} />

        <main className="flex-1 space-y-6 p-6">
          {seccion !== 'Inicio' ? (
            <SeccionEnDesarrollo titulo={seccion} />
          ) : (
            <>
              {/* Selector de fecha debajo de la navbar, a la derecha */}
              <div className="flex justify-end">
                <SelectorFecha etiqueta="Octubre 2026" />
              </div>

              {/* FILA 1: 4 KPI */}
              <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                <KpiCard
                  destacada
                  titulo="Ingresos Totales (COP)"
                  valor={formatCOP(m.ingresosTotales)}
                  detalle={`+${m.variacionIngresos}% este mes`}
                  sparkline={VENTAS_SEMANA_ADMIN.map((d) => d.valor)}
                />
                <KpiCard
                  titulo="Pedidos Completados"
                  valor={formatNumber(m.pedidosCompletados)}
                  detalle={`+${m.variacionPedidos}%`}
                />
                <KpiCard
                  titulo="Conductores Activos"
                  valor={
                    <>
                      {m.conductoresActivos}{' '}
                      <span className="text-base font-normal text-muted">/ {m.conductoresTotales}</span>
                    </>
                  }
                  detalle="online"
                />
                <KpiCard
                  titulo="Restaurantes Asociados"
                  valor={m.restaurantesAsociados}
                  detalle="partners"
                  detalleTono="gris"
                />
              </section>

              {/* FILA 2: gráfico de ventas (65%) + dona de categorías (35%) */}
              <section className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                <Card className="xl:col-span-2">
                  <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <h2 className="text-lg font-semibold">
                      {esSemanal ? 'Ventas de la Semana (COP)' : 'Ventas de Hoy (COP)'}
                    </h2>
                    {/* Selector Diario / Semanal */}
                    <div className="relative">
                      <select
                        value={periodo}
                        onChange={(e) => setPeriodo(e.target.value)}
                        className="appearance-none rounded-xl border border-line bg-app py-2 pl-4 pr-9 text-sm text-white focus:border-neon focus:outline-none"
                      >
                        <option>Diario</option>
                        <option>Semanal</option>
                      </select>
                      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted">▾</span>
                    </div>
                  </div>
                  {/* Escala 0 a 200.000 (la semanal); la diaria usa el mismo tope */}
                  <LineChart datos={datosVentas} maximo={200000} />
                </Card>

                <Card>
                  <h2 className="mb-4 text-lg font-semibold">Pedidos por Categoría</h2>
                  <DonutChart segmentos={CATEGORIAS_PEDIDOS} centro="100%" subtitulo="Total" />
                </Card>
              </section>

              {/* FILA 3: listas en vivo */}
              <section className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                {/* Conductores activos */}
                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Conductores Activos</h2>
                  <ul className="divide-y divide-line">
                    {CONDUCTORES_ACTIVOS.map((c) => (
                      <li key={c.id} className="flex items-center justify-between gap-3 py-3">
                        <div className="flex min-w-0 items-center gap-3">
                          <Avatar nombre={c.nombre} />
                          <span className="truncate font-medium">{c.nombre}</span>
                        </div>
                        {c.enEntrega ? (
                          <Badge tono="morado">En entrega (Pedido {c.pedido})</Badge>
                        ) : (
                          <Badge tono="verde">Disponible</Badge>
                        )}
                      </li>
                    ))}
                  </ul>
                </Card>

                {/* Gestión de restaurantes con switches */}
                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Gestión de Restaurantes</h2>
                  <ul className="divide-y divide-line">
                    {restaurantes.map((r) => (
                      <li key={r.id} className="flex items-center justify-between gap-3 py-3">
                        <span className="truncate font-medium">{r.nombre}</span>
                        <div className="flex items-center gap-3">
                          <Badge tono={r.activo ? 'verde' : 'rojo'}>
                            {r.activo ? `Activo${r.ventasHoy ? ` (${r.ventasHoy})` : ''}` : 'Inactivo (Cerrado)'}
                          </Badge>
                          <Toggle
                            checked={r.activo}
                            onChange={() => alternarRestaurante(r.id)}
                            label={`Activar o desactivar ${r.nombre}`}
                          />
                        </div>
                      </li>
                    ))}
                  </ul>
                </Card>
              </section>
            </>
          )}
        </main>
      </div>
    </div>
  );
}
