---
status: complete
phase: 01-gesti-n-del-inventario-por-el-admin
source: [01-VERIFICATION.md]
started: 2026-10-03T03:20:00Z
updated: 2026-10-03T16:45:00Z
---

## Current Test

[testing complete]

Las pruebas 3 a 7 las corrió Claude el 2026-10-03 en Chrome, contra el back local (base descartable `dante_uat_web`) y el front en el 5174 con `VITE_API_URL` local. La pestaña quedó en segundo plano (`document.hidden`) y Chrome no entregaba clics ni teclado reales, así que parte de las interacciones se dispararon por JS: `click()` en los botones, setter nativo más evento `input`/`change` en los campos y `requestSubmit()` en el form. Ejercitan los mismos handlers de React, pero no el puntero físico.

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
result: pass
note_web: "Crear desde el form (Citroën C4 Cactus Feel, USD) -> 'Publicación creada'; editar agencia (a Sucursal Pilar) y precio (14.900 -> 13.750) -> 'guardados', persiste al recargar y en la base; estado RESERVADO y VENDIDO desde el select de la fila, ambos siguen visibles en Todos; marcar destacado -> 'Quitar de destacados'; el destacado sigue en true después de cambiar el estado y de editar el kilometraje (base: RESERVADO, destacado t); filtros Todos(2) / Disponibles(0, 'No hay autos que coincidan con el filtro.') / Reservados(1) / Vendidos(1); búsqueda 'citroen', 'CITROËN', 'cactus' y 'peug' encuentran el auto correcto y 'xyz' no trae nada"

### 4. Eliminar un auto que tiene consultas y favoritos reales desde el panel
expected: El ConfirmDialog muestra solo cantidades, el botón Eliminar espera el conteo, el auto desaparece y no vuelve al recargar; no hay error de FK; los assets de Cloudinary desaparecen después del commit
result: pass
note_web: "Auto con 1 consulta y 1 favorito reales (creados por API como comprador): al abrir el diálogo el botón Eliminar arranca deshabilitado y se habilita al llegar el conteo; el texto es 'También se van a borrar 1 consulta de compradores y 1 favorito.' (solo cantidades); Eliminar cierra el diálogo, la fila desaparece y no vuelve al recargar; en la base quedan 0 publicaciones/consultas/favoritos de ese auto; sin errores de FK en el log. El borrado de assets de Cloudinary ya estaba verificado con credenciales reales"
note: "Parte API verificada por Claude contra el back real (base descartable), antes y después de los fixes del review: impacto 403 para comprador, admin ve {cantidadConsultas:1,cantidadFavoritos:1}; DELETE 204; luego 404; 0 filas en publicaciones/favoritos/consultas/fotos; sin errores de FK ni LazyInitialization (open-in-view=false). Falta: ConfirmDialog en navegador. Borrado de assets en Cloudinary verificado luego con credenciales reales (10 assets -> 404 tras borrar el auto)"

### 5. Con credenciales CLOUDINARY_* reales: subir JPG válida, PDF, imagen de 12-15 MB, .exe renombrado a .jpg, una tanda mixta; llegar a 10 fotos; reordenar y "Hacer portada"; eliminar la portada; recargar
expected: JPG aparece; PDF, >10 MB y .exe rechazados con mensaje claro; en la tanda cada archivo malo muestra "nombre: motivo" y los buenos se suben; la 11.ª foto no se puede subir; orden y portada persisten; al borrar la portada la siguiente pasa a orden 0 y el asset desaparece de Cloudinary; sin credenciales la API responde 502 en español
result: pass
note_web: "Sin credenciales CLOUDINARY_* en esta máquina. Tanda mixta desde el form (buena.jpg, manual.pdf, pesada.jpg de 12 MB, programa.jpg con bytes MZ): 'manual.pdf: formato no permitido (usá JPG, PNG o WebP)', 'pesada.jpg: pesa más de 10 MB', 'programa.jpg: El archivo no es una imagen válida (JPG, PNG o WebP)' y la buena 'No se pudo subir la imagen. Intentá de nuevo en unos minutos.' (502), sin fila guardada (0/10). Con filas de fotos cargadas en la base (sin public_id): 'Hacer portada' en la 3.ª -> c,a,b; flecha derecha en la 1.ª -> a,c,b; la flecha izquierda de la 1.ª está deshabilitada; persiste al recargar; borrar la portada -> quedan c (orden 0) y b (orden 1). Con 10 fotos el form muestra 10/10 y 'máximo por auto', sin selector de archivos. La subida exitosa a Cloudinary real quedó cubierta por la nota de API (mismo endpoint); no se repitió desde el form"
note: "Rechazos verificados por Claude contra el back real, antes y después de los fixes: PDF 400, .exe renombrado 400 (magic bytes), 12 y 15 MB 413 en español sin cortar la conexión, JPG válido sin credenciales 502 en español y sin fila guardada (con el lock pesimista de WR-01 activo); DELETE de foto en publicación inexistente 404. Falta con CLOUDINARY_* reales: subida, tope de 10, reorden/portada, borrado de portada y del asset, tanda mixta en el form, y dos subidas simultáneas a un auto con 9 fotos"
note_cloudinary: "Con Cloudinary real (base dante_uat, código con fixes WR-12/WR-13), por Claude vía API: 10 subidas 200 con public_id y asset accesible; la 11.ª 400 'Cada auto puede tener hasta 10 fotos'; reorden invertido 200 y persistido; orden incompleto 400; borrar la portada 204, el resto se resecuencia 0..8 y el asset da 404; dos subidas simultáneas con 9 fotos -> una 200 y una 400, quedan 10 fotos con órdenes 0..9 únicos; borrar el auto 204 y sus 10 assets dan 404. Falta solo la UI del form (tanda mixta, flechas, Hacer portada)"

### 6. Varias requests simultáneas con token vencido (el dashboard hace GET /agencias y GET /admin/publicaciones al montar)
expected: Una sola limpieza de sesión y una sola redirección a /login
result: pass
note_web: "Con un JWT vencido firmado con el secreto local: 4 requests protegidas simultáneas por el cliente api de la app (2x /admin/publicaciones, /impacto-eliminacion, /favoritos) -> 4 x 401, una sola limpieza de sesión y una sola navegación a /login con el aviso. Al montar el dashboard con token vencido, GET /agencias da 200 (es pública) y GET /admin/publicaciones da 401 (dos veces por StrictMode en dev) -> termina en /login con el aviso"

### 7. Sesión vencida en el panel y login fallido
expected: Con token vencido el panel lleva a /login con el aviso "Tu sesión venció…" y vuelve a la página de origen tras loguearse; un login fallido (401 de /auth/*) NO muestra el aviso ni redirige
result: pass
note_web: "Login con clave incorrecta: POST /auth/login 401, aparece 'Email o contraseña incorrectos', sin aviso de sesión ni redirección. Con token vencido en /admin/publicaciones/2/editar?tab=fotos, guardar -> PUT 401 -> /login con 'Tu sesión venció. Iniciá sesión de nuevo para seguir donde estabas.' y la sesión limpia; al loguearse vuelve a /admin/publicaciones/2/editar?tab=fotos. Las páginas cuyo GET es público (la edición) no detectan el vencimiento hasta la primera acción protegida"

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
