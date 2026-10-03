---
status: testing
phase: 01-gesti-n-del-inventario-por-el-admin
source: [01-VERIFICATION.md]
started: 2026-10-03T03:20:00Z
updated: 2026-10-03T03:20:00Z
---

## Current Test

number: 1
name: Arranque real con Postgres y ADMIN_*; login del admin
expected: |
  200 con rol ADMIN; la tabla agencias tiene 'Dante Automotores'/'dante-automotores'; reiniciar con otra ADMIN_PASSWORD no cambia la clave; con SPRING_PROFILES_ACTIVE=prod, sin ADMIN_* y sin admin, el arranque aborta con IllegalStateException
awaiting: user response

## Tests

### 1. Arranque real con Postgres (docker-compose) y ADMIN_EMAIL/ADMIN_PASSWORD/ADMIN_NOMBRE; POST /api/auth/login con esas credenciales
expected: 200 con rol ADMIN; la tabla agencias tiene 'Dante Automotores'/'dante-automotores'; reiniciar con otra ADMIN_PASSWORD no cambia la clave; con SPRING_PROFILES_ACTIVE=prod, sin ADMIN_* y sin admin, el arranque aborta con IllegalStateException
result: [pending]

### 2. Con ddl-auto: update sobre una base que ya tiene filas en publicaciones/fotos_publicacion, arrancar el back
expected: Se agregan sin error la columna destacado (boolean default false not null) y public_id; los autos viejos quedan destacado=false
result: [pending]

### 3. Panel completo contra la API real: crear un auto, editarlo (cambiar agencia, precio), recargar; cambiar estado a RESERVADO/VENDIDO; marcar y desmarcar destacado; filtrar por estado y buscar con acentos
expected: Todo persiste al recargar; un auto reservado/vendido sigue visible en el panel con su estado; el destacado no cambia al cambiar de estado ni al editar
result: [pending]

### 4. Eliminar un auto que tiene consultas y favoritos reales desde el panel
expected: El ConfirmDialog muestra solo cantidades, el botón Eliminar espera el conteo, el auto desaparece y no vuelve al recargar; no hay error de FK; los assets de Cloudinary desaparecen después del commit
result: [pending]

### 5. Con credenciales CLOUDINARY_* reales: subir JPG válida, PDF, imagen de 12-15 MB, .exe renombrado a .jpg, una tanda mixta; llegar a 10 fotos; reordenar y "Hacer portada"; eliminar la portada; recargar
expected: JPG aparece; PDF, >10 MB y .exe rechazados con mensaje claro; en la tanda cada archivo malo muestra "nombre: motivo" y los buenos se suben; la 11.ª foto no se puede subir; orden y portada persisten; al borrar la portada la siguiente pasa a orden 0 y el asset desaparece de Cloudinary; sin credenciales la API responde 502 en español
result: [pending]

### 6. Varias requests simultáneas con token vencido (el dashboard hace GET /agencias y GET /admin/publicaciones al montar)
expected: Una sola limpieza de sesión y una sola redirección a /login
result: [pending]

### 7. Sesión vencida en el panel y login fallido
expected: Con token vencido el panel lleva a /login con el aviso "Tu sesión venció…" y vuelve a la página de origen tras loguearse; un login fallido (401 de /auth/*) NO muestra el aviso ni redirige
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps
