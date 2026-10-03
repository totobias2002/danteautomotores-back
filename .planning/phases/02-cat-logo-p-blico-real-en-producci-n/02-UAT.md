---
status: partial
phase: 02-cat-logo-p-blico-real-en-producci-n
source: [02-01-SUMMARY.md, 02-05-SUMMARY.md, 02-06-SUMMARY.md, 02-07-SUMMARY.md, 02-08-PLAN.md]
started: 2026-10-03T21:40:00Z
updated: 2026-10-03T21:50:00Z
---

## Current Test

[testing paused — 1 item outstanding: 7 (producción, bloqueado hasta el deploy del plan 02-08)]

Las pruebas 1 a 6 las corrió Claude el 2026-10-03 en Chrome contra el back local (`scripts/verify/con-back-local.sh --copia-de dante_uat uat_fase2`, Flyway: baseline v1 → V2 → V3 sin errores) y el front en el 5174 con `VITE_API_URL=http://localhost:8080/api`. En la copia descartable se cargaron datos para ver todos los casos: tipos de carrocería, zonas de agencia, dos ofertas (`precio_anterior` mayor al precio), un segundo vendido con `fecha_vendido` de hace 3 días y 20 autos de relleno para tener más de 24. La pestaña quedó en segundo plano (`document.hidden`) y Chrome no entregaba clics ni teclado reales, así que parte de las interacciones se dispararon por JS (`click()`, setter nativo más evento `input`/`change`, `requestSubmit()`): ejercitan los mismos handlers de React, pero no el puntero físico. Las capturas que sí se pudieron sacar (Home, ficha vendida, ficha con oferta) se revisaron a ojo.

## Tests

### 1. Home con destacados reales (02-01)
expected: La Home muestra los autos destacados del backend (nunca vendidos), con la foto en la transformación de card de Cloudinary, el estado (reservado) y la oferta con el precio anterior tachado; "Ver todos" lleva a /autos
result: pass
note_web: "6 destacados reales, ningún vendido; imágenes `c_fill,w_640,h_420,q_auto,f_auto`; BMW con 'RESERVADO'; Corolla con badge 'Oferta', $ 26.900.000 tachado y $ 24.500.000; cada card muestra 'Zona · Agencia'. Sugerencias y 'Buscá por presupuesto' salen de las facetas (4 tramos de precio)"

### 2. Catálogo /autos: filtros, orden y paginación en la URL (02-04, 02-06)
expected: Filtros y orden se resuelven en el backend y quedan en la URL; paginado de a 24; atrás vuelve a la página anterior; un link compartido reproduce el listado; la búsqueda espera antes de pedir
result: pass
note_web: "Sin filtros: 31 resultados, página 1 con 24 y página 2 con 7 (Paginador con aria-current), scroll arriba al cambiar de página; history.back vuelve a la 1 con los mismos 24. Orden por defecto: destacados, después recientes, vendidos al final. SUV + Zona Oeste y 'Menor precio' → URL `?zona=ZONA_OESTE&orden=precio_asc`, 2 resultados ordenados por precio. `?ofertas=true` → solo las 2 ofertas. Link `?tipo=HATCHBACK&pagina=2` con 22 resultados (entran en una página) muestra la página 1 y limpia `pagina`. Búsqueda 'sandero': a los 150 y 350 ms no cambió la URL; a los ~950 ms quedó `busqueda=sandero` con 20 resultados"

### 3. Ficha del auto: disponible con oferta, reservado y vendido con parecidos (02-07, D-05, D-06)
expected: La ficha muestra el estado; el reservado se puede consultar; el vendido muestra 'Este auto ya se vendió', sin acción de consulta, y autos parecidos disponibles; la oferta muestra el precio anterior tachado
result: pass
note_web: "Vendido (208 Feline, vendido hace 3 días): etiqueta VENDIDO, aviso 'Este auto ya se vendió' con link a los disponibles, sin botones de consulta ni reserva, 'Autos parecidos' con el otro 208 disponible; ficha técnica con 'Tipo de carrocería' y 'Ubicación' (zona · agencia); galería 1/5 con 5 miniaturas. Reservado (BMW): etiqueta RESERVADO y 'Enviar consulta' / 'Reservar o agendar visita' visibles. Oferta (Corolla): badge OFERTA, $ 26.900.000 tachado (line-through) y $ 24.500.000. API: POST /consultas sobre un vendido → 400"

### 4. Página de agencia paginada (02-06)
expected: /agencias/:slug muestra los autos reales de esa agencia, paginados, con el título 'Autos (N)'
result: pass
note_web: "/agencias/autocity-belgrano: 'Autos (26)', 24 cards en la página 1 con Paginador; marcas de la agencia desde sus facetas"

### 5. Panel admin: zona de agencia, tipo de carrocería, precio anterior y fecha de venta (02-03, 02-05)
expected: El admin elige la zona al editar una agencia y el tipo y el precio anterior al editar un auto; los valores persisten al reabrir y guardar sin tocarlos no los borra; marcar VENDIDO registra la fecha de venta
result: pass
note_web: "Panel: chips de zona en la lista de agencias. Norte Motors: Zona Norte → Interior, persiste (base: INTERIOR). La agencia sembrada 'Dante Automotores' no tiene email, así que la validación HTML del form pide completarlo antes de guardar (esperado). Kangoo: tipo Utilitario → Monovolumen; precio anterior 12.000.000 (menor que el precio) muestra 'Con este valor el auto no se muestra como oferta.' y 17.900.000 lo quita; 'guardados.'; reabierto muestra Monovolumen y 17.900.000 y guardar sin tocar los conserva. Estado → VENDIDO desde la fila: base con `fecha_vendido` en ese momento; GET /similares devuelve 4 parecidos disponibles"

### 6. Créditos de imágenes (D-10)
expected: /creditos lista los créditos de las fotos de la demo y el pie de página tiene el link 'Créditos de imágenes'
result: pass
note_web: "/creditos con 'Créditos de imágenes', 100 links externos (original y licencia de cada foto); el footer enlaza a /creditos"

### 7. Sitio desplegado en producción contra la API productiva (PROD-02, PROD-04, D-09)
expected: Back en Railway con perfil prod (Flyway sobre la base existente, sin ddl-auto ni show-sql, arranque abortado sin secretos), front en Vercel, demo cargada con el script y el catálogo público funcionando contra la API productiva
result: blocked
blocked_by: third-party
reason: "Requiere acceso del usuario a Railway y Vercel: checkpoints humanos del plan 02-08 (H1-H2 relevamiento y backup, H3 ensayo local con la copia, H4-H9 deploy, demo y verificación)"

## Summary

total: 7
passed: 6
issues: 0
pending: 0
skipped: 0
blocked: 1

## Gaps
