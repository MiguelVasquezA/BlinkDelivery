package com.blinkdelivery.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializador y parser JSON mínimo, sin dependencias externas.
 * <ul>
 *   <li>{@link #escribir(Object)} convierte mapas, listas, cadenas, números, booleanos y {@code null} a JSON.</li>
 *   <li>{@link #leerObjeto(String)} convierte un texto JSON cuyo nivel raíz es un objeto en un {@code Map}.</li>
 * </ul>
 */
final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- escritura

    static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb);
        return sb.toString();
    }

    private static void escribir(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String texto) {
            escaparCadena(texto, sb);
        } else if (valor instanceof Boolean || valor instanceof Integer || valor instanceof Long) {
            sb.append(valor);
        } else if (valor instanceof Number numero) {
            double d = numero.doubleValue();
            sb.append(Double.isFinite(d) ? String.valueOf(d) : "null");
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escaparCadena(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escribir(e.getValue(), sb);
            }
            sb.append('}');
        } else if (valor instanceof Iterable<?> lista) {
            sb.append('[');
            boolean primero = true;
            for (Object o : lista) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                escribir(o, sb);
            }
            sb.append(']');
        } else {
            escaparCadena(valor.toString(), sb);
        }
    }

    private static void escaparCadena(String texto, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- lectura

    /**
     * Parsea un texto JSON cuya raíz es un objeto.
     *
     * @throws IllegalArgumentException si el texto no es un objeto JSON válido.
     */
    static Map<String, Object> leerObjeto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El cuerpo de la petición está vacío.");
        }
        Parser p = new Parser(texto);
        Object raiz = p.valor();
        p.saltarEspacios();
        if (!p.fin()) {
            throw p.error("texto sobrante después del objeto");
        }
        if (!(raiz instanceof Map)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON en el cuerpo de la petición.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> mapa = (Map<String, Object>) raiz;
        return mapa;
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        boolean fin() {
            return pos >= src.length();
        }

        void saltarEspacios() {
            while (!fin() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        IllegalArgumentException error(String detalle) {
            return new IllegalArgumentException("JSON inválido en la posición " + pos + ": " + detalle + ".");
        }

        Object valor() {
            saltarEspacios();
            if (fin()) {
                throw error("se esperaba un valor");
            }
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> objeto();
                case '[' -> arreglo();
                case '"' -> cadena();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numero();
            };
        }

        private Map<String, Object> objeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++; // '{'
            saltarEspacios();
            if (!fin() && src.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                if (fin() || src.charAt(pos) != '"') {
                    throw error("se esperaba una clave entre comillas");
                }
                String clave = cadena();
                saltarEspacios();
                if (fin() || src.charAt(pos) != ':') {
                    throw error("se esperaba ':'");
                }
                pos++;
                mapa.put(clave, valor());
                saltarEspacios();
                if (fin()) {
                    throw error("objeto sin cerrar");
                }
                char c = src.charAt(pos++);
                if (c == '}') {
                    return mapa;
                }
                if (c != ',') {
                    throw error("se esperaba ',' o '}'");
                }
            }
        }

        private List<Object> arreglo() {
            List<Object> lista = new ArrayList<>();
            pos++; // '['
            saltarEspacios();
            if (!fin() && src.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(valor());
                saltarEspacios();
                if (fin()) {
                    throw error("arreglo sin cerrar");
                }
                char c = src.charAt(pos++);
                if (c == ']') {
                    return lista;
                }
                if (c != ',') {
                    throw error("se esperaba ',' o ']'");
                }
            }
        }

        private String cadena() {
            pos++; // comilla inicial
            StringBuilder sb = new StringBuilder();
            while (!fin()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (fin()) {
                    break;
                }
                char e = src.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw error("escape \\u incompleto");
                        }
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("escape no válido '\\" + e + "'");
                }
            }
            throw error("cadena sin cerrar");
        }

        private Object literal(String palabra, Object valor) {
            if (!src.startsWith(palabra, pos)) {
                throw error("literal no válido");
            }
            pos += palabra.length();
            return valor;
        }

        private Double numero() {
            int inicio = pos;
            while (!fin() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) {
                pos++;
            }
            if (inicio == pos) {
                throw error("valor no esperado '" + src.charAt(pos) + "'");
            }
            try {
                return Double.parseDouble(src.substring(inicio, pos));
            } catch (NumberFormatException ex) {
                throw error("número no válido");
            }
        }
    }
}
