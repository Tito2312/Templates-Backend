-- =====================================================================
-- SAC Plantillas 1.0.4 - Rollback de la migración 002 (catálogo global)
-- Solo elimina los índices únicos de nombre. Las copias duplicadas que se
-- fusionaron NO se recuperan aquí: para eso hay que restaurar el backup.
-- Las plantillas no se ven afectadas (siguen apuntando a la copia conservada).
-- =====================================================================

BEGIN;

DROP INDEX IF EXISTS brands_name_uk;
DROP INDEX IF EXISTS categories_name_uk;

COMMIT;
