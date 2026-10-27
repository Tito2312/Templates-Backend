# Migraciones de base de datos

El esquema de Supabase se gestiona manualmente (`spring.jpa.hibernate.ddl-auto=none`).
Cada cambio de esquema se versiona aquí y se ejecuta a mano en el SQL Editor de Supabase.

## Convención

- `V<versión>_<nnn>__<descripcion>.sql`: migración. Debe ser idempotente.
- `rollback/R<versión>_<nnn>__<descripcion>.sql`: reversión de la migración con el mismo número.

## Orden de despliegue

1. Ejecutar las migraciones pendientes, en orden, **antes** de desplegar el backend.
2. Comprobar las consultas de verificación del final de cada script.
3. Desplegar el backend.

> El SQL Editor de Supabase solo muestra el resultado de la **última** sentencia ejecutada.
> Para ver una vista previa o una verificación, selecciona solo esa consulta y ejecútala sola.

## Registro

| Script | Versión | Descripción | Ejecutado en producción |
|---|---|---|---|
| `V1_0_4_001__add_user_role.sql` | 1.0.4 | Columna `users.role` (`ROLE_USER`/`ROLE_ADMIN`) y admin inicial | Parcial: la columna se creó a mano. Ejecutar el script completo (idempotente) para el CHECK y el admin |
| `V1_0_4_002__global_catalog.sql` | 1.0.4 | Fusión de marcas/categorías duplicadas e índices únicos de nombre | Pendiente (justo antes del despliegue, con backup) |
| `V1_0_4_003__template_usage.sql` | 1.0.4 | Tabla `template_usage`: contador de uso por usuario | Pendiente (antes del despliegue) |
