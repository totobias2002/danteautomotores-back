---
status: partial
phase: 01-gesti-n-del-inventario-por-el-admin
source: [01-VERIFICATION.md]
started: 2026-10-03T03:20:00Z
updated: 2026-10-03T05:25:00Z
---

## Current Test

[testing paused — 5 items outstanding: 3, 6, 7 (navegador), 4 (ConfirmDialog en navegador), 5 (Cloudinary real)]

## Tests

### 1. Arranque real con Postgres (docker-compose) y ADMIN_EMAIL/ADMIN_PASSWORD/ADMIN_NOMBRE; POST /api/auth/login con esas credenciales
expected: 200 con rol ADMIN; la tabla agencias tiene 'Dante Automotores'/'dante-automotores'; reiniciar con otra ADMIN_PASSWORD no cambia la clave; con SPRING_PROFILES_ACTIVE=prod, sin ADMIN_* y sin admin, el arranque aborta con IllegalStateException
result: pass
note: "Corrido por Claude contra Postgres real (base descartable dante_uat): seed crea agencia y admin, login 200 ADMIN; reinicio con otra clave no la cambia (vieja 200, nueva 401); perfil prod sin ADMIN_* aborta con IllegalStateException (exit 1)"

### 2. Con ddl-auto: update sobre una base que ya tiene filas en publicaciones/fotos_publicacion, arrancar el back
expected: Se agregan sin error la columna destacado (boolean default false not null) y public_id; los autos viejos quedan destacado=false
result: pass
note: "Corrido por Claude sobre la base de dev (1 auto existente): ddl-auto agregó destacado boolean NOT NULL DEFAULT false y public_id varchar nullable; el auto viejo quedó destacado=false; sin errores en el log"

### 3. Panel completo contra la API real: crear un auto, editarlo (cambiar agencia, precio), recargar; cambiar estado a RESERVADO/VENDIDO; marcar y desmarcar destacado; filtrar por estado y buscar con acentos
expected: Todo persiste al recargar; un auto reservado/vendido sigue visible en el panel con su estado; el destacado no cambia al cambiar de estado ni al editar
result: [pending]

### 4. Eliminar un auto que tiene consultas y favoritos reales desde el panel
expected: El ConfirmDialog muestra solo cantidades, el botón Eliminar espera el conteo, el auto desaparece y no vuelve al recargar; no hay error de FK; los assets de Cloudinary desaparecen después del commit
result: [pending]
note: "Parte API verificada por Claude contra el back real (base descartable), antes y después de los fixes del review: impacto 403 para comprador, admin ve {cantidadConsultas:1,cantidadFavoritos:1}; DELETE 204; luego 404; 0 filas en publicaciones/favoritos/consultas/fotos; sin errores de FK ni LazyInitialization (open-in-view=false). Falta: ConfirmDialog en navegador y borrado de assets en Cloudinary (requiere credenciales)"

### 5. Con credenciales CLOUDINARY_* reales: subir JPG válida, PDF, imagen de 12-15 MB, .exe renombrado a .jpg, una tanda mixta; llegar a 10 fotos; reordenar y "Hacer portada"; eliminar la portada; recargar
expected: JPG aparece; PDF, >10 MB y .exe rechazados con mensaje claro; en la tanda cada archivo malo muestra "nombre: motivo" y los buenos se suben; la 11.ª foto no se puede subir; orden y portada persisten; al borrar la portada la siguiente pasa a orden 0 y el asset desaparece de Cloudinary; sin credenciales la API responde 502 en español
result: [pending]
note: "Rechazos verificados por Claude contra el back real, antes y después de los fixes: PDF 400, .exe renombrado 400 (magic bytes), 12 y 15 MB 413 en español sin cortar la conexión, JPG válido sin credenciales 502 en español y sin fila guardada (con el lock pesimista de WR-01 activo); DELETE de foto en publicación inexistente 404. Falta con CLOUDINARY_* reales: subida, tope de 10, reorden/portada, borrado de portada y del asset, tanda mixta en el form, y dos subidas simultáneas a un auto con 9 fotos"

### 6. Varias requests simultáneas con token vencido (el dashboard hace GET /agencias y GET /admin/publicaciones al montar)
expected: Una sola limpieza de sesión y una sola redirección a /login
result: [pending]

### 7. Sesión vencida en el panel y login fallido
expected: Con token vencido el panel lleva a /login con el aviso "Tu sesión venció…" y vuelve a la página de origen tras loguearse; un login fallido (401 de /auth/*) NO muestra el aviso ni redirige
result: [pending]

## Summary

total: 7
passed: 2
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
