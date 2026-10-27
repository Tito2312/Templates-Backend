-- =====================================================================
-- SAC Plantillas 1.0.4 - Rollback de la migración 003 (uso por usuario)
-- Ejecutar SOLO después de volver a desplegar un backend que no use template_usage.
-- Se pierden los contadores de uso por usuario registrados desde el despliegue.
-- =====================================================================

BEGIN;

DROP TABLE IF EXISTS template_usage;

COMMIT;
