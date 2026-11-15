/** @type {import('tailwindcss').Config} */
// Configuración de Tailwind: aquí viven los colores exactos del tema
// oscuro con acentos neón (morado / fucsia) de BLINK DELIVERY.
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        app: '#0D0D11',        // Fondo principal (negro profundo)
        card: '#16161D',       // Tarjetas, Sidebar y Navbar
        line: '#272732',       // Bordes sutiles
        neon: '#A855F7',       // Morado neón (acento principal)
        'neon-deep': '#9D00FF',// Morado intenso (botones / degradados)
        fuchsia: '#D946EF',    // Fucsia (gráficos y degradados)
        pink: '#EC4899',       // Rosa (gráficos)
        lilac: '#C084FC',      // Lila (gráficos y textos de acento)
        ok: '#22C55E',         // Verde: activo / disponible / en camino
        warn: '#EAB308',       // Ámbar: preparando
        err: '#EF4444',        // Rojo: inactivo / rechazar
        muted: '#9CA3AF',      // Gris claro para subtítulos y etiquetas
      },
      boxShadow: {
        neon: '0 0 15px rgba(157, 0, 255, 0.35)',
        'neon-fuerte': '0 0 25px rgba(168, 85, 247, 0.55)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
