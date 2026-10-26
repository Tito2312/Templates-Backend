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

## Registro

| Script | Versión | Descripción | Ejecutado en producción |
|---|---|---|---|
| `V1_0_4_001__add_user_role.sql` | 1.0.4 | Columna `users.role` (`ROLE_USER`/`ROLE_ADMIN`) y admin inicial | Pendiente |
