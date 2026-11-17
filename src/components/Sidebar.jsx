// ============================================================
// Sidebar.jsx — Barra lateral reutilizable
// Cambia sus ítems según el rol activo (MENUS en mockData.js).
// ============================================================
import {
  BarChart3,
  Bike,
  CircleDollarSign,
  Headphones,
  Heart,
  LayoutDashboard,
  Package,
  Power,
  Receipt,
  Settings,
  ShoppingBag,
  Store,
  Tag,
  User,
  Wallet,
} from 'lucide-react';
import { MENUS } from '../data/mockData.js';
import Logo from './Logo.jsx';

// Icono de cada ítem del menú (se busca por el texto del ítem)
const ICONOS = {
  Inicio: LayoutDashboard,
  Conductores: Bike,
  Restaurantes: Store,
  Pedidos: ShoppingBag,
  Análisis: BarChart3,
  Finanzas: Wallet,
  Configuración: Settings,
  Favoritos: Heart,
  'Mis Pedidos': Receipt,
  Promociones: Tag,
  Cuenta: User,
  Disponibilidad: Power,
  'Mis Entregas': Package,
  Ganancias: CircleDollarSign,
  Soporte: Headphones,
};

export default function Sidebar({ role, activeItem, onSelect }) {
  // Ítems del rol actual
  const items = MENUS[role] ?? [];

  return (
    <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col border-r border-line bg-card p-5">
      {/* Logo arriba */}
      <div className="mb-8 px-2">
        <Logo tamano="md" />
      </div>

      {/* Lista de navegación: el ítem activo tiene borde morado neón */}
      <nav className="flex flex-col gap-1.5">
        {items.map((item) => {
          const Icono = ICONOS[item] ?? LayoutDashboard;
          const activo = item === activeItem;
          return (
            <button
              key={item}
              type="button"
              onClick={() => onSelect(item)}
              className={`flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition ${
                activo
                  ? 'border-neon bg-neon/10 text-white shadow-neon'
                  : 'border-transparent text-muted hover:bg-line/50 hover:text-white'
              }`}
            >
              <Icono size={18} className={activo ? 'text-neon' : ''} />
              {item}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
