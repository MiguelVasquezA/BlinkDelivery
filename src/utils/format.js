// ============================================================
// format.js — Utilidades de formato numérico (pesos colombianos)
// ============================================================

// Separa miles con punto: 145890200 -> "145.890.200"
export function formatNumber(valor) {
  return Math.round(valor)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

// Agrega el símbolo de peso: 185500 -> "$185.500"
export function formatCOP(valor) {
  return `$${formatNumber(valor)}`;
}
