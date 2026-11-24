-- 009: barrio de la dirección y fechas con hora.
--
-- BORRADOR: no se ha ejecutado en Supabase. Ejecútalo a mano después de 008.
-- Es aditiva salvo los dos cambios de tipo, que convierten las fechas existentes
-- interpretándolas como hora de Bogotá (America/Bogota), la zona de negocio.

-- 1. Barrio de la dirección (opcional).
ALTER TABLE direccion
    ADD COLUMN IF NOT EXISTS barrio VARCHAR(100) NULL;

-- 2. Fecha y hora del pedido. Antes era DATE (solo día).
ALTER TABLE pedido
    ALTER COLUMN fecha_pedido TYPE TIMESTAMPTZ
    USING (fecha_pedido::timestamp AT TIME ZONE 'America/Bogota');

-- 3. Fecha y hora de la calificación. Antes era DATE (solo día).
ALTER TABLE calificacion
    ALTER COLUMN fecha TYPE TIMESTAMPTZ
    USING (fecha::timestamp AT TIME ZONE 'America/Bogota');
