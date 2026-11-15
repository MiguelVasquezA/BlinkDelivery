// Configuración de Vite: compila React con el plugin oficial (JSX + Fast Refresh)
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
});
