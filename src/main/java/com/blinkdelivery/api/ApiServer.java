package com.blinkdelivery.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servidor HTTP de BlinkDelivery (sin frameworks, sobre {@code com.sun.net.httpserver}).
 *
 * <p>Puerto: variable de entorno {@code PORT} (por defecto 8080), como exige Render.
 * <p>Los DAO leen {@code data/*.txt} con rutas relativas al directorio de trabajo, así que
 * el servidor debe arrancarse desde la raíz del proyecto (el {@code Dockerfile} ya lo hace).
 */
public final class ApiServer {

    private static final int PUERTO_DEFECTO = 8080;

    private ApiServer() {
    }

    public static void main(String[] args) throws IOException {
        int puerto = puertoDesdeEntorno();
        ApiRoutes rutas = new ApiRoutes();

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/", intercambio -> atender(intercambio, rutas));
        // Un solo hilo: los DAO leen y reescriben archivos completos sin sincronización.
        servidor.setExecutor(null);
        servidor.start();

        System.out.println("BlinkDelivery API escuchando en el puerto " + puerto);
        System.out.println("Directorio de trabajo (data/ se resuelve desde aquí): " + Path.of("").toAbsolutePath());
    }

    private static int puertoDesdeEntorno() {
        String valor = System.getenv("PORT");
        if (valor == null || valor.isBlank()) {
            return PUERTO_DEFECTO;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La variable PORT no es un número válido: '" + valor + "'.", e);
        }
    }

    private static void atender(HttpExchange intercambio, ApiRoutes rutas) throws IOException {
        try {
            agregarCors(intercambio);
            String metodo = intercambio.getRequestMethod().toUpperCase(Locale.ROOT);

            // Preflight de CORS: el navegador lo envía antes de PUT, DELETE o POST con JSON.
            if (metodo.equals("OPTIONS")) {
                enviar(intercambio, 204, null);
                return;
            }

            String ruta = intercambio.getRequestURI().getRawPath();
            if (ruta.equals("/")) {
                enviar(intercambio, 200, Map.of("servicio", "BlinkDelivery API", "base", "/api"));
                return;
            }
            if (!ruta.equals("/api") && !ruta.startsWith("/api/")) {
                enviar(intercambio, 404, Map.of("error", "Ruta no encontrada. La API está bajo /api."));
                return;
            }

            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ApiRoutes.Respuesta respuesta = rutas.manejar(metodo, segmentos(ruta), cuerpo);
            enviar(intercambio, respuesta.estado(), respuesta.cuerpo());

        } catch (ApiRoutes.ApiException e) {
            enviar(intercambio, e.estado, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            enviar(intercambio, 400, Map.of("error", e.getMessage()));
        } catch (IOException e) {
            System.err.println("Error de archivos: " + e);
            enviar(intercambio, 500, Map.of("error", "Error al acceder a los datos."));
        } catch (RuntimeException e) {
            System.err.println("Error inesperado: " + e);
            e.printStackTrace();
            enviar(intercambio, 500, Map.of("error", "Error interno del servidor."));
        }
    }

    /** Convierte {@code /api/customers/123} en {@code [customers, 123]}. */
    private static List<String> segmentos(String ruta) {
        List<String> seg = new ArrayList<>();
        for (String parte : ruta.substring("/api".length()).split("/")) {
            if (!parte.isEmpty()) {
                seg.add(URLDecoder.decode(parte, StandardCharsets.UTF_8));
            }
        }
        return seg;
    }

    /** CORS abierto: cualquier origen puede consumir la API desde un frontend externo. */
    private static void agregarCors(HttpExchange intercambio) {
        var cabeceras = intercambio.getResponseHeaders();
        cabeceras.set("Access-Control-Allow-Origin", "*");
        cabeceras.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        cabeceras.set("Access-Control-Allow-Headers", "Content-Type");
        cabeceras.set("Access-Control-Max-Age", "86400");
    }

    private static void enviar(HttpExchange intercambio, int estado, Object cuerpo) throws IOException {
        if (cuerpo == null) {
            intercambio.sendResponseHeaders(estado, -1); // sin contenido
            intercambio.close();
            return;
        }
        byte[] bytes = Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
