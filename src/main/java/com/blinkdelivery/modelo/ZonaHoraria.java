package com.blinkdelivery.modelo;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Hora de negocio de BlinkDelivery (Colombia, America/Bogota, UTC-5 sin horario de verano).
 * <p>
 * Los pedidos, calificaciones e historiales se guardan como {@link LocalDateTime} sin zona horaria.
 * Si se usa {@code LocalDateTime.now()} directamente, se toma la hora del servidor: en Render el
 * servidor corre en UTC, así que los pedidos quedaban registrados 5 horas adelantados.
 * Por eso toda hora nueva del sistema debe obtenerse con {@link #ahora()}.
 * </p>
 */
public final class ZonaHoraria {

    /** Zona horaria de la operación. */
    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private ZonaHoraria() {
    }

    /** Fecha y hora actuales en la zona de Bogotá, sin zona explícita (formato guardado en los archivos). */
    public static LocalDateTime ahora() {
        return LocalDateTime.now(BOGOTA);
    }
}
