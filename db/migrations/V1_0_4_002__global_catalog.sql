-- =====================================================================
-- SAC Plantillas 1.0.4 - Migración 002: catálogo global
-- Marcas y categorías pasan a ser compartidas por todos los usuarios,
-- así que su nombre debe ser único (sin distinguir mayúsculas ni espacios).
--
-- Ejecutar MANUALMENTE en Supabase JUSTO ANTES de desplegar el backend 1.0.4
-- (la 1.0.3 filtra por usuario: tras fusionar, un usuario dejaría de ver
-- la copia que se elimina hasta que se despliegue la 1.0.4).
--
-- HACER UN BACKUP ANTES: el rollback no puede recuperar las copias eliminadas.
--
-- Conserva TODAS las plantillas: solo se mueven a la copia que se conserva.
-- Criterio por grupo de nombres repetidos: la copia con más plantillas;
-- si empatan, la de id más bajo (la más antigua).
-- Idempotente: si no hay duplicados no hace nada.
-- =====================================================================

-- 0. VISTA PREVIA (ejecutar sola primero): qué copia se conserva y cuáles se eliminan
WITH ranked AS (
    SELECT 'marca' AS tipo, b.id, b.name, lower(trim(b.name)) AS clave,
           (SELECT count(*) FROM templates t WHERE t.brand_id = b.id) AS plantillas
    FROM brands b
    UNION ALL
    SELECT 'categoria', c.id, c.name, lower(trim(c.name)),
           (SELECT count(*) FROM templates t WHERE t.category_id = c.id)
    FROM categories c
), ordered AS (
    SELECT *, row_number() OVER (PARTITION BY tipo, clave ORDER BY plantillas DESC, id ASC) AS pos,
              count(*)     OVER (PARTITION BY tipo, clave) AS copias
    FROM ranked
)
SELECT tipo, clave, id, name, plantillas,
       CASE WHEN pos = 1 THEN 'SE CONSERVA' ELSE 'SE ELIMINA (sus plantillas pasan a la conservada)' END AS accion
FROM ordered
WHERE copias > 1
ORDER BY tipo, clave, pos;


BEGIN;

-- 1. Marcas: mapa copia sobrante -> copia conservada
CREATE TEMP TABLE brand_merge ON COMMIT DROP AS
WITH ranked AS (
    SELECT b.id, lower(trim(b.name)) AS clave,
           row_number() OVER (
               PARTITION BY lower(trim(b.name))
               ORDER BY (SELECT count(*) FROM templates t WHERE t.brand_id = b.id) DESC, b.id ASC
           ) AS pos
    FROM brands b
)
SELECT d.id AS duplicate_id, k.id AS keeper_id
FROM ranked d
JOIN ranked k ON k.clave = d.clave AND k.pos = 1
WHERE d.pos > 1;

UPDATE templates t
SET brand_id = m.keeper_id
FROM brand_merge m
WHERE t.brand_id = m.duplicate_id;

DELETE FROM brands b
USING brand_merge m
WHERE b.id = m.duplicate_id;

-- 2. Categorías: mismo procedimiento
CREATE TEMP TABLE category_merge ON COMMIT DROP AS
WITH ranked AS (
    SELECT c.id, lower(trim(c.name)) AS clave,
           row_number() OVER (
               PARTITION BY lower(trim(c.name))
               ORDER BY (SELECT count(*) FROM templates t WHERE t.category_id = c.id) DESC, c.id ASC
           ) AS pos
    FROM categories c
)
SELECT d.id AS duplicate_id, k.id AS keeper_id
FROM ranked d
JOIN ranked k ON k.clave = d.clave AND k.pos = 1
WHERE d.pos > 1;

UPDATE templates t
SET category_id = m.keeper_id
FROM category_merge m
WHERE t.category_id = m.duplicate_id;

DELETE FROM categories c
USING category_merge m
WHERE c.id = m.duplicate_id;

-- 3. Normalizar espacios sobrantes en los nombres conservados
UPDATE brands     SET name = trim(name) WHERE name <> trim(name);
UPDATE categories SET name = trim(name) WHERE name <> trim(name);

-- 4. Unicidad de nombres a partir de ahora
CREATE UNIQUE INDEX IF NOT EXISTS brands_name_uk     ON brands     (lower(trim(name)));
CREATE UNIQUE INDEX IF NOT EXISTS categories_name_uk ON categories (lower(trim(name)));

COMMIT;

-- 5. Verificación: no deben quedar duplicados ni plantillas apuntando a ids inexistentes
SELECT 'marca' AS tipo, lower(trim(name)) AS clave, count(*) FROM brands GROUP BY 2 HAVING count(*) > 1
UNION ALL
SELECT 'categoria', lower(trim(name)), count(*) FROM categories GROUP BY 2 HAVING count(*) > 1;

SELECT count(*) AS plantillas_totales,
       count(*) FILTER (WHERE brand_id IS NOT NULL AND brand_id NOT IN (SELECT id FROM brands))             AS marca_huerfana,
       count(*) FILTER (WHERE category_id IS NOT NULL AND category_id NOT IN (SELECT id FROM categories)) AS categoria_huerfana
FROM templates;
