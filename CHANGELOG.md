# Changelog — Backend SAC Plantillas

## [1.0.4]

> Requiere ejecutar las migraciones `db/migrations/V1_0_4_001`, `002` y `003` **antes** de desplegar.
> Ver `db/migrations/DEPLOY_1.0.4.md`.

### Seguridad y roles
- Nuevos roles `ROLE_ADMIN` y `ROLE_USER` (columna `users.role`). Los usuarios existentes quedan como `ROLE_USER`; el admin inicial se asigna por migración.
- El registro siempre crea usuarios `ROLE_USER`; el rol no se puede enviar ni manipular desde el cliente.
- El rol se lee de la BD en cada petición: los tokens actuales siguen siendo válidos y un cambio de rol aplica al instante.
- Permisos en servidor: consultar y copiar plantillas, marcas y categorías → cualquier usuario autenticado; crear, editar y eliminar, y subir imágenes → solo admin.
- `GET /api/users/{id}` restringido a admin (antes exponía datos de cualquier usuario).
- Respuestas 401 (sin sesión válida) y 403 (sin permiso) en JSON. Antes un anónimo recibía 403.
- Eliminados los logs de diagnóstico que imprimían el email en cada petición.
- Eliminada la configuración CORS duplicada (`WebConfig` con `*`); se mantienen solo los orígenes reales.

### Nuevos endpoints
- `GET /api/users/me`: datos del usuario autenticado con su rol.

### Catálogo global
- Plantillas, marcas y categorías son visibles para todos los usuarios (`user_id` queda como creador).
- Nombres de marca y categoría únicos, sin distinguir mayúsculas ni espacios (409 si se repiten).
- Eliminar una marca o categoría ya no falla: sus plantillas se conservan y quedan sin ella.
- El filtro por marca o categoría no distingue mayúsculas.
- Paginación acotada (`page ≥ 0`, `size` de 1 a 50); tamaño por defecto 12.

### Uso de plantillas por usuario
- El contador de uso es individual (tabla `template_usage`): cada usuario ve y ordena por sus propios usos.
- Copiar ya no modifica la plantilla (antes cambiaba su fecha de actualización).
- `templates.usage_count` se conserva sin uso como histórico global.

### Errores y validación
- Manejador global de errores con formato único `{timestamp, status, error, message, path}`: 400 validación, 404 no encontrado, 409 conflicto, 403 sin permiso, 500 genérico sin detalles internos.
- Validación de datos de entrada (registro, login, plantillas, marcas, categorías).
- Registro: email o nombre de usuario repetidos → 409 con mensaje claro (antes devolvía el error SQL).

### Interno
- Pruebas automáticas: 38 (permisos por rol, servicios, consultas en H2). `./mvnw test` ya no necesita una BD real.
- Versión del proyecto alineada: `1.0.4`.
