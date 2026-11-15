// ============================================================
// Navbar.jsx — Barra superior reutilizable
// Título de la vista, buscador (opcional), notificaciones, mensajes
// y menú de perfil con "Cerrar Sesión".
// ============================================================
import { useEffect, useRef, useState } from 'react';
import { Bell, ChevronDown, LogOut, MessageSquare, Search } from 'lucide-react';

export default function Navbar({ titulo, buscador = false, usuario, correo, onLogout, children }) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const [hayNotificaciones, setHayNotificaciones] = useState(true);
  const menuRef = useRef(null);

  // Cierra el menú de perfil cuando se hace clic fuera de él
  useEffect(() => {
    function cerrarSiFueraDeMenu(evento) {
      if (menuRef.current && !menuRef.current.contains(evento.target)) {
        setMenuAbierto(false);
      }
    }
    document.addEventListener('mousedown', cerrarSiFueraDeMenu);
    return () => document.removeEventListener('mousedown', cerrarSiFueraDeMenu);
  }, []);

  // Iniciales del avatar (ej: "admin" -> "AD")
  const iniciales = usuario.slice(0, 2).toUpperCase();

  return (
    <header className="sticky top-0 z-30 flex flex-wrap items-center gap-4 border-b border-line bg-card/90 px-6 py-4 backdrop-blur">
      {/* Título de la vista */}
      <h1 className="text-xl font-bold text-white">{titulo}</h1>

      <div className="ml-auto flex items-center gap-3">
        {/* Buscador (solo en las vistas que lo necesitan) */}
        {buscador && (
          <div className="relative hidden md:block">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar en el panel..."
              className="w-64 rounded-xl border border-line bg-app py-2 pl-9 pr-4 text-sm text-white placeholder:text-muted focus:border-neon focus:outline-none"
            />
          </div>
        )}

        {/* Elementos extra que cada vista quiera mostrar (ej: fecha) */}
        {children}

        {/* Notificaciones: al hacer clic se quita el punto de aviso */}
        <button
          type="button"
          aria-label="Notificaciones"
          onClick={() => setHayNotificaciones(false)}
          className="relative rounded-xl border border-line p-2 text-muted transition hover:border-neon hover:text-white"
        >
          <Bell size={18} />
          {hayNotificaciones && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-fuchsia" />}
        </button>

        {/* Mensajes */}
        <button
          type="button"
          aria-label="Mensajes"
          className="rounded-xl border border-line p-2 text-muted transition hover:border-neon hover:text-white"
        >
          <MessageSquare size={18} />
        </button>

        {/* Perfil: abre un menú con "Cerrar Sesión" */}
        <div ref={menuRef} className="relative">
          <button
            type="button"
            onClick={() => setMenuAbierto((abierto) => !abierto)}
            aria-expanded={menuAbierto}
            className="flex items-center gap-2 rounded-xl border border-line px-3 py-2 text-sm text-white transition hover:border-neon"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-neon-deep to-fuchsia text-xs font-bold">
              {iniciales}
            </span>
            <span>{usuario}</span>
            <ChevronDown size={16} className="text-muted" />
          </button>

          {menuAbierto && (
            <div className="absolute right-0 mt-2 w-56 rounded-xl border border-line bg-card p-2 shadow-neon">
              <div className="mb-1 border-b border-line px-3 py-2">
                <p className="text-sm font-semibold">{usuario}</p>
                {correo && <p className="truncate text-xs text-muted">{correo}</p>}
              </div>
              <button
                type="button"
                onClick={onLogout}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-err transition hover:bg-err/10"
              >
                <LogOut size={16} />
                Cerrar Sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
