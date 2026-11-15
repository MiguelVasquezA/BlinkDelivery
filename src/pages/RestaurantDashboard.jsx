// ============================================================
// RestaurantDashboard.jsx — Panel del Restaurante Asociado
// Filas: 4 KPI · ventas + platos más vendidos · pedidos activos + menú
// ============================================================
import { useState } from 'react';
import Sidebar from '../components/Sidebar.jsx';
import Navbar from '../components/Navbar.jsx';
import {
  Badge,
  Card,
  DonutChart,
  KpiCard,
  LineChart,
  SeccionEnDesarrollo,
  SelectorFecha,
  Toast,
  Toggle,
  useMensajeTemporal,
} from '../components/Widgets.jsx';
import {
  MENU_RESTAURANTE,
  METRICAS_RESTAURANTE,
  PEDIDOS_RESTAURANTE,
  PLATOS_MAS_VENDIDOS,
  ROLES,
  VENTAS_SEMANA_RESTAURANTE,
} from '../data/mockData.js';
import { formatCOP, formatNumber } from '../utils/format.js';

// Texto y color de cada estado de pedido del restaurante
const ESTADOS_PEDIDO = {
  nuevo: { texto: 'Nuevo', tono: 'morado' },
  preparando: { texto: 'En preparación', tono: 'ambar' },
  listo: { texto: 'Listo para recoger', tono: 'verde' },
};

export default function RestaurantDashboard({ correo, onLogout }) {
  const [seccion, setSeccion] = useState('Inicio');
  const [pedidos, setPedidos] = useState(PEDIDOS_RESTAURANTE);
  const [menu, setMenu] = useState(MENU_RESTAURANTE);
  const [mensaje, mostrarMensaje] = useMensajeTemporal();

  const m = METRICAS_RESTAURANTE;

  // Cambia el estado de un pedido (ej: nuevo -> preparando)
  function cambiarEstadoPedido(id, nuevoEstado) {
    setPedidos((lista) => lista.map((p) => (p.id === id ? { ...p, estado: nuevoEstado } : p)));
  }

  // Quita el pedido de la lista activa
  function rechazarPedido(id) {
    setPedidos((lista) => lista.filter((p) => p.id !== id));
    mostrarMensaje(`Pedido #${id} rechazado`);
  }

  // Activa o desactiva un plato del menú
  function alternarPlato(id) {
    setMenu((lista) => lista.map((p) => (p.id === id ? { ...p, activo: !p.activo } : p)));
  }

  return (
    <div className="flex min-h-screen bg-app text-white">
      <Sidebar role={ROLES.RESTAURANT} activeItem={seccion} onSelect={setSeccion} />

      <div className="flex min-w-0 flex-1 flex-col">
        <Navbar
          titulo="Panel del Restaurante Asociado"
          buscador
          usuario="Restaurante"
          correo={correo}
          onLogout={onLogout}
        >
          <SelectorFecha etiqueta="Octubre 2026" />
        </Navbar>

        <main className="flex-1 space-y-6 p-6">
          {seccion !== 'Inicio' ? (
            <SeccionEnDesarrollo titulo={seccion} />
          ) : (
            <>
              {/* FILA 1: 4 KPI */}
              <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                <KpiCard
                  destacada
                  titulo="Mis Ingresos (COP)"
                  valor={formatCOP(m.ingresos)}
                  detalle={`+${m.variacionIngresos}% este mes`}
                  sparkline={VENTAS_SEMANA_RESTAURANTE.map((d) => d.valor)}
                />
                <KpiCard
                  titulo="Pedidos Hoy"
                  valor={
                    <>
                      {m.pedidosHoy}{' '}
                      <span className="text-base font-normal text-muted">Completados</span>
                    </>
                  }
                  detalle={`+${m.variacionPedidos}%`}
                />
                <KpiCard titulo="Valor Promedio Pedido" valor={formatCOP(m.valorPromedio)} detalle="por pedido" detalleTono="gris" />
                <KpiCard titulo="Items Vendidos Hoy" valor={formatNumber(m.itemsVendidos)} detalle="unidades" detalleTono="gris" />
              </section>

              {/* FILA 2: ventas semanales (2/3) + platos más vendidos (1/3) */}
              <section className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                <Card className="xl:col-span-2">
                  <h2 className="mb-4 text-lg font-semibold">Gráfico de Ventas (COP)</h2>
                  {/* Escala 0 a 8.000.000 */}
                  <LineChart datos={VENTAS_SEMANA_RESTAURANTE} maximo={8000000} />
                </Card>

                <Card>
                  <h2 className="mb-4 text-lg font-semibold">Platos Más Vendidos</h2>
                  <DonutChart segmentos={PLATOS_MAS_VENDIDOS} centro="100%" subtitulo="Ventas" />
                </Card>
              </section>

              {/* FILA 3: pedidos activos + gestión del menú */}
              <section className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Pedidos Activos</h2>
                  {pedidos.length === 0 && <p className="py-6 text-center text-sm text-muted">No hay pedidos activos.</p>}
                  <ul className="divide-y divide-line">
                    {pedidos.map((p) => {
                      const estado = ESTADOS_PEDIDO[p.estado];
                      return (
                        <li key={p.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                          <div>
                            <p className="font-medium">
                              Pedido #{p.id} - <Badge tono={estado.tono}>{estado.texto}</Badge>
                            </p>
                            <p className="text-xs text-muted">({p.cliente})</p>
                          </div>

                          {/* Botones según el estado del pedido */}
                          {p.estado === 'nuevo' && (
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() => cambiarEstadoPedido(p.id, 'preparando')}
                                className="rounded-lg bg-ok px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                              >
                                Aceptar
                              </button>
                              <button
                                type="button"
                                onClick={() => rechazarPedido(p.id)}
                                className="rounded-lg bg-err px-3 py-1.5 text-xs font-semibold text-white transition hover:brightness-110"
                              >
                                Rechazar
                              </button>
                            </div>
                          )}
                          {p.estado === 'preparando' && (
                            <button
                              type="button"
                              onClick={() => cambiarEstadoPedido(p.id, 'listo')}
                              className="btn-outline px-3 py-1.5 text-xs"
                            >
                              Marcar listo
                            </button>
                          )}
                          {p.estado === 'listo' && <span className="text-xs text-muted">Esperando domiciliario</span>}
                        </li>
                      );
                    })}
                  </ul>
                </Card>

                <Card>
                  <h2 className="mb-2 text-lg font-semibold">Gestión del Menú</h2>
                  <ul className="divide-y divide-line">
                    {menu.map((plato) => (
                      <li key={plato.id} className="flex items-center justify-between gap-3 py-3">
                        <div>
                          <p className="font-medium">{plato.nombre}</p>
                          <p className="text-xs text-muted">
                            {plato.activo ? `Activo (${plato.vendidosHoy} hoy)` : 'Inactivo'}
                          </p>
                        </div>
                        <Toggle
                          checked={plato.activo}
                          onChange={() => alternarPlato(plato.id)}
                          label={`Activar o desactivar ${plato.nombre}`}
                        />
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
