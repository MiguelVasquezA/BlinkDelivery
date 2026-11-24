-- 007: calificación por tipo (restaurante y domiciliario) y horarios del restaurante.
--
-- BORRADOR: no se ha ejecutado en Supabase. Revísalo y ejecútalo a mano.
-- Antes, comprueba el nombre real de la restricción única de id_pedido:
--   select conname, pg_get_constraintdef(oid)
--   from pg_constraint where conrelid = 'calificacion'::regclass;
-- Si el nombre no es calificacion_por_pedido_unica, ajusta el DROP CONSTRAINT.

-- 1. Domiciliario calificado (NULL = calificación al restaurante).
ALTER TABLE calificacion
    ADD COLUMN IF NOT EXISTS id_domiciliario INTEGER NULL
        REFERENCES domiciliario (id_domiciliario) ON DELETE RESTRICT;

-- 2. Un pedido ya no admite una sola calificación: una al restaurante y otra al domiciliario.
ALTER TABLE calificacion
    DROP CONSTRAINT IF EXISTS calificacion_por_pedido_unica;

CREATE UNIQUE INDEX IF NOT EXISTS calificacion_restaurante_unica
    ON calificacion (id_pedido)
    WHERE id_domiciliario IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS calificacion_domiciliario_unica
    ON calificacion (id_pedido, id_domiciliario)
    WHERE id_domiciliario IS NOT NULL;

-- 3. Horarios del restaurante (campos que usa el modelo Java y no existen en la tabla).
ALTER TABLE restaurante
    ADD COLUMN IF NOT EXISTS hora_apertura TIME NULL,
    ADD COLUMN IF NOT EXISTS hora_cierre TIME NULL;
