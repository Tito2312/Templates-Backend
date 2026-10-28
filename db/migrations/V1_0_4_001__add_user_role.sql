-- =====================================================================
-- SAC Plantillas 1.0.4 - Migración 001: rol de usuario
-- Ejecutar MANUALMENTE en Supabase (SQL Editor) ANTES de desplegar
-- el backend 1.0.4. Es aditiva e idempotente: la versión 1.0.3 sigue
-- funcionando con la columna nueva, y puede ejecutarse más de una vez.
-- =====================================================================

BEGIN;

-- 1. Columna de rol: todos los usuarios existentes quedan como ROLE_USER
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';

-- 2. Solo se permiten los roles conocidos por la aplicación
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_role_chk'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT users_role_chk CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'));
    END IF;
END $$;

-- 3. Administrador(es) inicial(es), por email
UPDATE users
SET role = 'ROLE_ADMIN'
WHERE lower(trim(email)) IN ('jorge@atomogaming.com');

COMMIT;

-- 4. Verificación (debe listar al menos un ROLE_ADMIN)
SELECT role, count(*) FROM users GROUP BY role;
SELECT id, username, email, role FROM users WHERE role = 'ROLE_ADMIN';
