// ============================================================
// Login.jsx — Pantalla "BLINK DELIVERY - Acceso al Sistema"
// Reglas:
//  - Las 3 tarjetas visibles (Cliente, Domiciliario, Restaurante) llevan
//    a su panel con cualquier correo/contraseña.
//  - El admin NO aparece como tarjeta. Entra si el correo es admin@blink.com
//    o si se activa el modo oculto con doble clic sobre el logo.
// ============================================================
import { useState } from 'react';
import { Bell, Bike, Eye, EyeOff, Lock, Mail, Store, User } from 'lucide-react';
import Logo from '../components/Logo.jsx';
import { ADMIN_EMAIL, ROLES } from '../data/mockData.js';

// Tarjetas visibles de selección de cuenta (NO incluye admin)
const OPCIONES_CUENTA = [
  { rol: ROLES.CUSTOMER, etiqueta: 'Soy Cliente', Icono: User },
  { rol: ROLES.DRIVER, etiqueta: 'Soy Domiciliario', Icono: Bike },
  { rol: ROLES.RESTAURANT, etiqueta: 'Soy Restaurante', Icono: Store },
];

// Clases comunes de los campos de texto
const CAMPO =
  'w-full rounded-xl border border-line bg-app py-3 pl-10 pr-10 text-sm text-white placeholder:text-muted/70 transition focus:border-neon focus:outline-none focus:shadow-neon';

export default function Login({ onLogin }) {
  const [rolSeleccionado, setRolSeleccionado] = useState(ROLES.CUSTOMER);
  const [correo, setCorreo] = useState('');
  const [clave, setClave] = useState('');
  const [verClave, setVerClave] = useState(false);
  const [recordarme, setRecordarme] = useState(false); // solo visual, no persiste
  // Modo admin oculto: se activa/desactiva con doble clic sobre el logo
  const [modoAdminOculto, setModoAdminOculto] = useState(false);

  // Al presionar INGRESAR decide a qué panel entrar
  function handleSubmit(evento) {
    evento.preventDefault();
    const correoLimpio = correo.trim().toLowerCase();

    // Regla admin: correo fijo o modo oculto activado -> AdminDashboard
    const esAdmin = modoAdminOculto || correoLimpio === ADMIN_EMAIL;

    onLogin({
      rol: esAdmin ? ROLES.ADMIN : rolSeleccionado,
      correo: correoLimpio,
    });
  }

  return (
    <div className="flex min-h-screen flex-col bg-app">
      {/* Cabecera: logo a la izquierda, campana y avatar a la derecha */}
      <header className="flex items-center justify-between border-b border-line bg-card/60 px-8 py-4">
        <Logo tamano="sm" />
        <div className="flex items-center gap-4">
          <button
            type="button"
            aria-label="Notificaciones"
            className="rounded-xl border border-line p-2 text-muted transition hover:border-neon hover:text-white"
          >
            <Bell size={18} />
          </button>
          <div className="flex items-center gap-2">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-neon-deep to-fuchsia text-xs font-bold">
              BD
            </span>
            <span className="text-sm font-semibold">BLINK DELIVERY</span>
          </div>
        </div>
      </header>

      <main className="flex flex-1 items-center justify-center p-6">
        {/* Tarjeta central con borde morado brillante */}
        <form
          onSubmit={handleSubmit}
          className="w-full max-w-md rounded-3xl border border-neon bg-card p-8 shadow-neon-fuerte"
        >
          {/* Logo grande. Doble clic (secreto) activa/desactiva el modo admin */}
          <div
            className="mb-6 flex select-none justify-center"
            onDoubleClick={() => setModoAdminOculto((activo) => !activo)}
          >
            <Logo tamano="lg" />
          </div>

          <h2 className="text-center text-2xl font-bold">Iniciar Sesión</h2>
          <p className="mb-6 mt-1 text-center text-sm text-muted">
            Selecciona tu tipo de cuenta para continuar
          </p>

          {/* Selector de las 3 cuentas visibles */}
          <div className="mb-6 grid grid-cols-3 gap-3">
            {OPCIONES_CUENTA.map(({ rol, etiqueta, Icono }) => {
              const seleccionada = rolSeleccionado === rol;
              return (
                <button
                  key={rol}
                  type="button"
                  onClick={() => setRolSeleccionado(rol)}
                  aria-pressed={seleccionada}
                  className={`flex flex-col items-center gap-2 rounded-xl border px-2 py-4 text-center text-xs font-medium transition ${
                    seleccionada
                      ? 'border-neon bg-neon/10 text-white shadow-neon'
                      : 'border-line text-muted hover:border-neon/50 hover:text-white'
                  }`}
                >
                  <Icono size={22} className={seleccionada ? 'text-neon' : ''} />
                  {etiqueta}
                </button>
              );
            })}
          </div>

          {/* Correo */}
          <div className="relative mb-4">
            <Mail size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
            <input
              type="email"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="ejemplo@blink.com"
              aria-label="Correo electrónico"
              className={CAMPO}
            />
          </div>

          {/* Contraseña con botón de ojo para mostrar/ocultar */}
          <div className="relative mb-4">
            <Lock size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
            <input
              type={verClave ? 'text' : 'password'}
              value={clave}
              onChange={(e) => setClave(e.target.value)}
              placeholder="Contraseña"
              aria-label="Contraseña"
              className={CAMPO}
            />
            <button
              type="button"
              onClick={() => setVerClave((v) => !v)}
              aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-white"
            >
              {verClave ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>

          {/* Recordarme + enlace de contraseña olvidada */}
          <div className="mb-6 flex items-center justify-between text-sm">
            <label className="flex cursor-pointer items-center gap-2 text-muted">
              <input
                type="checkbox"
                checked={recordarme}
                onChange={(e) => setRecordarme(e.target.checked)}
                className="h-4 w-4 accent-[#9D00FF]"
              />
              Recordarme
            </label>
            {/* Enlace decorativo: no hay recuperación de contraseña en la demo */}
            <a href="#" onClick={(e) => e.preventDefault()} className="font-medium text-neon hover:underline">
              ¡Olvidé mi contraseña!
            </a>
          </div>

          {/* Botón de acceso con degradado morado vibrante */}
          <button
            type="submit"
            className="w-full rounded-xl bg-gradient-to-r from-neon-deep to-fuchsia py-3 text-sm font-bold tracking-widest text-white shadow-neon transition hover:brightness-110 active:scale-[0.99]"
          >
            INGRESAR
          </button>

          {/* Pie de tarjeta */}
          <div className="mt-6 flex justify-center gap-4 text-xs text-muted">
            <a href="#" onClick={(e) => e.preventDefault()} className="hover:text-neon">
              Soporte
            </a>
            <span>•</span>
            <a href="#" onClick={(e) => e.preventDefault()} className="hover:text-neon">
              Términos y condiciones
            </a>
          </div>
        </form>
      </main>
    </div>
  );
}
