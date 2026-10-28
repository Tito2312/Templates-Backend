-- =====================================================================
-- SAC Plantillas 1.0.4 - Rollback de la migración 001 (rol de usuario)
-- Ejecutar SOLO después de volver a desplegar el backend 1.0.3:
-- el backend 1.0.4 necesita la columna "role" para autenticar.
-- =====================================================================

BEGIN;

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_chk;
ALTER TABLE users DROP COLUMN IF EXISTS role;

COMMIT;
