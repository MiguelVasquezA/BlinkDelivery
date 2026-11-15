// ============================================================
// Logo.jsx — Logo de BLINK DELIVERY: moto morada en neón + texto
// ============================================================

// tamano: 'sm' (cabecera), 'md' (sidebar), 'lg' (tarjeta de login)
export default function Logo({ tamano = 'md', mostrarTexto = true, className = '' }) {
  const tamanoIcono = { sm: 'h-7 w-9', md: 'h-10 w-14', lg: 'h-20 w-28' }[tamano];
  const tamanoTitulo = { sm: 'text-sm', md: 'text-lg', lg: 'text-3xl' }[tamano];

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      {/* Icono de moto dibujado en SVG con trazo morado y resplandor */}
      <svg
        viewBox="0 0 64 44"
        className={`${tamanoIcono} shrink-0 drop-shadow-[0_0_8px_rgba(157,0,255,0.9)]`}
        aria-hidden="true"
      >
        <g fill="none" stroke="#A855F7" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
          {/* Ruedas */}
          <circle cx="14" cy="32" r="9" />
          <circle cx="50" cy="32" r="9" />
          {/* Chasis */}
          <path d="M14 32 L24 18 H38 L50 32" />
          <path d="M24 18 L30 32 H38" />
          {/* Manubrio */}
          <path d="M38 18 L42 10 H48" />
        </g>
      </svg>

      {/* Texto del logo */}
      {mostrarTexto && (
        <div className="leading-tight">
          <p className={`font-black tracking-wide text-white ${tamanoTitulo}`}>BLINK</p>
          <p className="text-xs font-semibold tracking-[0.3em] text-neon">DELIVERY</p>
        </div>
      )}
    </div>
  );
}
