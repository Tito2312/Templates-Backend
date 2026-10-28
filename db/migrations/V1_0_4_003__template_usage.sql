-- =====================================================================
-- SAC Plantillas 1.0.4 - Migración 003: uso de plantillas por usuario
-- El contador de uso deja de ser global: cada usuario tiene el suyo.
-- Ejecutar MANUALMENTE en Supabase ANTES de desplegar el backend 1.0.4
-- (después de la 001 y la 002).
--
-- Aditiva e idempotente: solo crea una tabla nueva. No modifica datos.
-- templates.usage_count se conserva (sin uso en la app) como histórico global.
-- Todos los usuarios empiezan con 0 usos.
-- =====================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS template_usage (
    user_id      BIGINT    NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    template_id  BIGINT    NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    usage_count  INTEGER   NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, template_id)
);

-- Para borrar rápido los usos de una plantilla eliminada
CREATE INDEX IF NOT EXISTS template_usage_template_idx ON template_usage (template_id);

COMMIT;

-- Verificación: la tabla existe (y está vacía la primera vez)
SELECT count(*) AS registros_de_uso FROM template_usage;
