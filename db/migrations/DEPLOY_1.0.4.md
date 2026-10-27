# Guía de despliegue — SAC Plantillas 1.0.4

Orden obligatorio: **BD → backend → frontend**. El backend 1.0.4 no arranca bien sin las migraciones
(con `ddl-auto=none`, sin la columna `users.role` falla el login), y el frontend 1.0.4 necesita el backend 1.0.4.

Las migraciones son aditivas: la 1.0.3 sigue funcionando después de la **001** y la **003**.
La **002** fusiona marcas/categorías duplicadas; con la 1.0.3 todavía activa, un usuario podría dejar
de ver alguna copia hasta desplegar la 1.0.4, por eso se ejecuta **justo antes** del despliegue.

---

## 0. Antes de empezar

- [ ] Ramas `release/1.0.4` de Backend y frontend probadas en local y subidas a `origin`.
- [ ] **Render — backend**: confirmar que existen las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
      `SPRING_DATASOURCE_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` y `JWT_SECRET`.
      (1.0.4 no añade variables nuevas.)
      Si `JWT_SECRET` **no** existe se usa el secreto por defecto del código: funciona igual que en 1.0.3,
      pero queda pendiente en la tarea de secretos.
- [ ] **Render — frontend (Static Site)**: `VITE_API_URL` apunta al backend (`https://<backend>/api`) y existe la regla
      **Rewrite** `/* → /index.html`.
- [ ] Avisar a los usuarios: tras el despliegue los contadores de uso empiezan en 0 (ahora son personales) y solo
      el administrador puede crear o editar plantillas, marcas y categorías.

## 1. Backup (Supabase)

- Database → Backups: confirmar que existe un backup reciente, **o** exportar a CSV las tablas
  `users`, `templates`, `brands` y `categories`.

## 2. Migraciones (Supabase → SQL Editor)

> El SQL Editor solo muestra el resultado de la **última** sentencia. Para ver una vista previa o una
> verificación, selecciona solo esa consulta y ejecútala sola.

1. **`V1_0_4_001__add_user_role.sql`** — ejecutar completo (es idempotente; la columna puede existir ya).
   - Verificar: `SELECT role, count(*) FROM users GROUP BY role;` → al menos 1 `ROLE_ADMIN`.
   - Verificar: `SELECT conname FROM pg_constraint WHERE conname = 'users_role_chk';` → 1 fila.
2. **`V1_0_4_002__global_catalog.sql`**
   - Ejecutar **solo el paso 0 (vista previa)** y revisar. Con los datos de octubre de 2026 se esperaba conservar
     `CHAT`(2), `Correo`(1), `BET593`(3) y `LOTBET`(2) sin mover plantillas. Si aparecen otros grupos, revisar antes de seguir.
   - Ejecutar el script completo.
   - Verificar (final del script): 0 duplicados y `marca_huerfana = 0`, `categoria_huerfana = 0`.
3. **`V1_0_4_003__template_usage.sql`** — ejecutar completo.
   - Verificar: `SELECT count(*) FROM template_usage;` → 0 la primera vez.

## 3. Backend

1. PR `release/1.0.4 → main` del repo Backend y merge.
2. Render despliega `main`. Esperar a que el servicio quede *Live*.
3. Comprobar `https://<backend>/api/health` → `OK`.
4. Comprobar que sin token una ruta protegida responde **401** (p. ej. `GET /api/templates`).

## 4. Frontend

1. PR `release/1.0.4 → main` del repo frontend y merge.
2. Render publica el Static Site.
3. Recargar con caché limpia (Ctrl+F5).

## 5. Prueba rápida en producción

- [ ] Abrir `/dashboard` sin sesión → lleva a la página de inicio.
- [ ] Recargar directamente `/dashboard` con sesión → no da 404.
- [ ] Entrar como **admin** (`jorge@atomogaming.com`) → ve "Nueva Plantilla", "Categorías", editar y eliminar.
- [ ] Entrar como **usuario** → solo ve copiar; copiar sube su contador y reordena la lista.
- [ ] Admin: crear una marca con un nombre existente → mensaje de duplicado.
- [ ] Más de 12 plantillas → aparece la paginación.

## 6. Plan de vuelta atrás

1. Volver a desplegar la versión anterior en Render (frontend y backend: *Manual Deploy* del commit previo o revert del merge).
   Con eso la app vuelve a la 1.0.3; las tablas/columnas nuevas no le molestan.
2. Solo si hace falta dejar la BD como antes (tras desplegar la 1.0.3), en este orden:
   `rollback/R1_0_4_003`, `rollback/R1_0_4_002`, `rollback/R1_0_4_001`.
   - R002 solo quita los índices únicos: las copias duplicadas eliminadas **se recuperan únicamente desde el backup**.
   - R003 borra los contadores de uso por usuario registrados desde el despliegue.

## 7. Pendientes conocidos (fuera de 1.0.4)

- Rotar secretos (Cloudinary, JWT, contraseña de BD) y quitar los valores por defecto de `application.properties`
  y `JwtTokenProvider`. Requiere confirmar antes las variables de Render.
- Imágenes guardadas en base64 en `templates.image_url`; `/api/images/upload` (Cloudinary) existe pero no se usa → 1.0.5.
- Render gratuito: el backend tarda en despertar; la app muestra la pantalla de "Reintentar".
- `templates.usage_count` queda sin uso (histórico global); se puede eliminar en una versión futura.
