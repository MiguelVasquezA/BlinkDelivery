// ============================================================
// Widgets.jsx — Piezas visuales reutilizables de los dashboards:
// tarjetas, KPI, badges, toggles, gráficos SVG (línea, dona, sparkline),
// avatar, selector de fecha, mensajes temporales y placeholder de sección.
// Los gráficos se dibujan a mano con SVG (sin librerías externas).
// ============================================================
import { useEffect, useId, useState } from 'react';
import { Calendar, ChevronDown } from 'lucide-react';
import { formatCOP, formatNumber } from '../utils/format.js';

// ---------- Tarjeta base ----------
// destacada = true añade el borde morado neón con resplandor
export function Card({ children, destacada = false, className = '' }) {
  return (
    <div
      className={`rounded-2xl border bg-card p-5 ${
        destacada ? 'border-neon shadow-neon' : 'border-line'
      } ${className}`}
    >
      {children}
    </div>
  );
}

// ---------- Badge (etiqueta de estado) ----------
const TONOS = {
  verde: 'border-ok/40 bg-ok/15 text-ok',
  ambar: 'border-warn/40 bg-warn/15 text-warn',
  rojo: 'border-err/40 bg-err/15 text-err',
  morado: 'border-neon/40 bg-neon/15 text-lilac',
  gris: 'border-line bg-line text-muted',
};

export function Badge({ tono = 'gris', children }) {
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-medium ${TONOS[tono]}`}
    >
      {children}
    </span>
  );
}

// ---------- Avatar con iniciales ----------
export function Avatar({ nombre }) {
  const iniciales = nombre
    .split(' ')
    .slice(0, 2)
    .map((p) => p[0])
    .join('')
    .toUpperCase();
  return (
    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-neon-deep to-fuchsia text-xs font-bold">
      {iniciales}
    </span>
  );
}

// ---------- Tarjeta KPI (indicador clave) ----------
// valor: número/texto/elemento. detalle: línea pequeña inferior.
// sparkline: array de números para dibujar una mini gráfica.
export function KpiCard({ titulo, valor, detalle, detalleTono = 'verde', destacada = false, sparkline }) {
  return (
    <Card destacada={destacada}>
      <p className="text-sm text-muted">{titulo}</p>
      <div className="mt-2 flex flex-wrap items-end justify-between gap-2">
        <div className="min-w-0">
          <p className="whitespace-nowrap text-2xl font-bold text-white">{valor}</p>
          {detalle && (
            <p className={`mt-1 text-sm ${detalleTono === 'verde' ? 'text-ok' : 'text-muted'}`}>{detalle}</p>
          )}
        </div>
        {sparkline && <Sparkline datos={sparkline} />}
      </div>
    </Card>
  );
}

// Convierte una lista de puntos {x, y} en una curva suave (Bézier cúbica)
export function crearCurva(puntos) {
  if (puntos.length === 0) return '';
  let d = `M ${puntos[0].x} ${puntos[0].y}`;
  for (let i = 1; i < puntos.length; i++) {
    const anterior = puntos[i - 1];
    const actual = puntos[i];
    const medioX = (anterior.x + actual.x) / 2; // puntos de control a mitad de camino
    d += ` C ${medioX} ${anterior.y}, ${medioX} ${actual.y}, ${actual.x} ${actual.y}`;
  }
  return d;
}

// ---------- Sparkline (mini gráfica de tendencia) ----------
export function Sparkline({ datos, color = '#A855F7', ancho = 110, alto = 40 }) {
  const min = Math.min(...datos);
  const max = Math.max(...datos);
  const rango = max - min || 1;
  const puntos = datos.map((valor, i) => ({
    x: (i / (datos.length - 1)) * ancho,
    y: alto - ((valor - min) / rango) * (alto - 6) - 3,
  }));
  return (
    <svg
      width={ancho}
      height={alto}
      viewBox={`0 0 ${ancho} ${alto}`}
      className="shrink-0 drop-shadow-[0_0_6px_rgba(157,0,255,0.8)]"
      aria-hidden="true"
    >
      <path d={crearCurva(puntos)} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
    </svg>
  );
}

// ---------- Toggle (switch morado) ----------
export function Toggle({ checked, onChange, label }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`relative h-6 w-11 shrink-0 rounded-full transition ${
        checked ? 'bg-neon-deep shadow-neon' : 'bg-line'
      }`}
    >
      <span
        className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
          checked ? 'translate-x-5' : ''
        }`}
      />
    </button>
  );
}

// ---------- Gráfico de línea con brillo neón ----------
// datos: [{ etiqueta, valor }]. maximo: valor del tope del eje Y.
export function LineChart({ datos, maximo, color = '#A855F7', formatoEje = formatNumber, altura = 280 }) {
  // useId genera un id único (evita conflictos si hay varios gráficos)
  const uid = useId().replace(/:/g, '');
  const [indiceActivo, setIndiceActivo] = useState(null);

  // Dimensiones del lienzo SVG y márgenes internos
  const W = 720;
  const H = altura;
  const PL = 90; // margen izquierdo (etiquetas del eje Y)
  const PR = 20;
  const PT = 20;
  const PB = 36; // margen inferior (etiquetas del eje X)
  const anchoUtil = W - PL - PR;
  const altoUtil = H - PT - PB;

  // Convierte cada valor en coordenadas del SVG
  const puntos = datos.map((d, i) => ({
    ...d,
    x: PL + (i / (datos.length - 1)) * anchoUtil,
    y: PT + altoUtil - (d.valor / maximo) * altoUtil,
  }));
  const curva = crearCurva(puntos);
  const yBase = PT + altoUtil;
  const area = `${curva} L ${puntos[puntos.length - 1].x} ${yBase} L ${puntos[0].x} ${yBase} Z`;
  const marcas = [0, 0.25, 0.5, 0.75, 1];
  const activo = indiceActivo !== null ? puntos[indiceActivo] : null;

  return (
    <div className="relative w-full">
      <svg viewBox={`0 0 ${W} ${H}`} className="h-auto w-full" onMouseLeave={() => setIndiceActivo(null)}>
        <defs>
          {/* Degradado del área bajo la curva */}
          <linearGradient id={`area-${uid}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.45" />
            <stop offset="100%" stopColor={color} stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* Líneas de cuadrícula y valores del eje Y */}
        {marcas.map((f) => {
          const y = yBase - f * altoUtil;
          return (
            <g key={f}>
              <line x1={PL} x2={W - PR} y1={y} y2={y} stroke="#272732" strokeDasharray="4 6" />
              <text x={PL - 10} y={y + 4} textAnchor="end" fill="#9CA3AF" fontSize="13">
                {formatoEje(maximo * f)}
              </text>
            </g>
          );
        })}

        {/* Etiquetas del eje X */}
        {puntos.map((p) => (
          <text key={p.etiqueta} x={p.x} y={H - 10} textAnchor="middle" fill="#9CA3AF" fontSize="13">
            {p.etiqueta}
          </text>
        ))}

        {/* Área y curva con resplandor */}
        <path d={area} fill={`url(#area-${uid})`} />
        <path
          d={curva}
          fill="none"
          stroke={color}
          strokeWidth="3"
          strokeLinecap="round"
          className="drop-shadow-[0_0_8px_rgba(157,0,255,0.9)]"
        />

        {/* Puntos interactivos: al pasar el mouse muestran el valor */}
        {puntos.map((p, i) => (
          <g key={p.etiqueta} onMouseEnter={() => setIndiceActivo(i)}>
            <circle cx={p.x} cy={p.y} r="14" fill="transparent" />
            <circle
              cx={p.x}
              cy={p.y}
              r={indiceActivo === i ? 6 : 3.5}
              fill={color}
              stroke="#0D0D11"
              strokeWidth="2"
            />
          </g>
        ))}
      </svg>

      {/* Tooltip flotante con el valor del punto activo */}
      {activo && (
        <div
          className="pointer-events-none absolute -translate-x-1/2 -translate-y-full rounded-lg border border-neon bg-card px-3 py-1.5 text-xs shadow-neon"
          style={{ left: `${(activo.x / W) * 100}%`, top: `${(activo.y / H) * 100}%` }}
        >
          <p className="text-muted">{activo.etiqueta}</p>
          <p className="font-semibold text-white">{formatCOP(activo.valor)}</p>
        </div>
      )}
    </div>
  );
}

// ---------- Gráfico de dona ----------
// segmentos: [{ etiqueta, valor, color }]. Los valores son porcentajes/partes.
export function DonutChart({ segmentos, centro, subtitulo }) {
  const RADIO = 70;
  const GROSOR = 24;
  const CIRCUNFERENCIA = 2 * Math.PI * RADIO;
  const total = segmentos.reduce((suma, s) => suma + s.valor, 0);

  let acumulado = 0; // longitud ya dibujada (para colocar el siguiente arco)

  return (
    <div className="flex flex-col items-center gap-5">
      <svg viewBox="0 0 200 200" className="h-48 w-48">
        {/* Pista de fondo */}
        <circle cx="100" cy="100" r={RADIO} fill="none" stroke="#272732" strokeWidth={GROSOR} />

        {/* Un arco por segmento */}
        {segmentos.map((seg) => {
          const longitud = (seg.valor / total) * CIRCUNFERENCIA;
          const arco = (
            <circle
              key={seg.etiqueta}
              cx="100"
              cy="100"
              r={RADIO}
              fill="none"
              stroke={seg.color}
              strokeWidth={GROSOR}
              strokeDasharray={`${longitud} ${CIRCUNFERENCIA - longitud}`}
              strokeDashoffset={-acumulado}
              transform="rotate(-90 100 100)"
              className="drop-shadow-[0_0_6px_rgba(157,0,255,0.6)]"
            />
          );
          acumulado += longitud;
          return arco;
        })}

        {/* Texto central */}
        <text x="100" y="98" textAnchor="middle" fill="#FFFFFF" fontSize="22" fontWeight="700">
          {centro}
        </text>
        <text x="100" y="118" textAnchor="middle" fill="#9CA3AF" fontSize="12">
          {subtitulo}
        </text>
      </svg>

      {/* Leyenda */}
      <ul className="w-full space-y-2 text-sm">
        {segmentos.map((seg) => (
          <li key={seg.etiqueta} className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-muted">
              <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: seg.color }} />
              {seg.etiqueta}
            </span>
            <span className="font-semibold text-white">{seg.valor}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ---------- Selector de fecha (decorativo) ----------
export function SelectorFecha({ etiqueta }) {
  return (
    <button
      type="button"
      className="inline-flex items-center gap-2 rounded-xl border border-line bg-card px-4 py-2 text-sm transition hover:border-neon"
    >
      <Calendar size={16} className="text-neon" />
      {etiqueta}
      <ChevronDown size={16} className="text-muted" />
    </button>
  );
}

// ---------- Placeholder para secciones del menú aún sin contenido ----------
export function SeccionEnDesarrollo({ titulo }) {
  return (
    <Card className="py-16 text-center">
      <p className="text-lg font-semibold text-white">{titulo}</p>
      <p className="mt-2 text-sm text-muted">Esta sección está en desarrollo. Vuelve a “Inicio” para ver el panel.</p>
    </Card>
  );
}

// ---------- Mensajes temporales (toast) ----------
// Hook: devuelve [mensaje, mostrarMensaje]. El mensaje desaparece a los 2.5 s.
export function useMensajeTemporal() {
  const [mensaje, setMensaje] = useState('');
  useEffect(() => {
    if (!mensaje) return undefined;
    const temporizador = setTimeout(() => setMensaje(''), 2500);
    return () => clearTimeout(temporizador);
  }, [mensaje]);
  return [mensaje, setMensaje];
}

// Componente visual del mensaje (esquina inferior derecha)
export function Toast({ mensaje }) {
  if (!mensaje) return null;
  return (
    <div className="fixed bottom-6 right-6 z-50 rounded-xl border border-neon bg-card px-4 py-3 text-sm font-medium text-white shadow-neon">
      {mensaje}
    </div>
  );
}
