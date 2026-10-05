# Roadmap: Dante Automotores

## Overview

El proyecto parte de un backend y un front que ya existen. El catálogo público todavía usa mocks, y la compra y la cotización se resuelven por WhatsApp. El orden de las fases es este:

1. El inventario real queda en manos de la única cuenta admin.
2. El catálogo real sale a producción.
3. Se exigen cuentas con identidad completa (teléfono y DNI).
4. Sobre esa base se construyen las dos operaciones del negocio dentro de la web: comprar un auto conversando con la agencia y cotizar el usado propio.
5. Al final viene el pulido visual medido que pidió el negocio.

Cada fase es un corte vertical (back en `danteautomotores-back` + front en `danteautomotores-front`) que se puede verificar de punta a punta.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Gestión del inventario por el admin** - La única cuenta admin gestiona autos, fotos, estados y destacados desde el panel contra la API real (completed 2026-10-03)
- [x] **Phase 2: Catálogo público real en producción** - El visitante navega el catálogo real (paginado, filtrado, con destacados y estados) en el sitio desplegado (completed 2026-10-05)
- [ ] **Phase 3: Cuentas verificadas** - Registro con teléfono y DNI, login con Google con perfil completo, recuperación de contraseña y acciones protegidas
- [ ] **Phase 4: Compra por conversación con la agencia** - "Lo quiero" abre una conversación de compra; usuario y admin conversan en bandejas con no leídos y avisos por mail
- [ ] **Phase 5: Cotizador de usados** - El usuario cotiza su auto paso a paso, recibe un precio estimado y lo usa para vender o como parte de pago
- [ ] **Phase 6: Experiencia visual pulida** - Microinteracciones, aparición al scrollear, galería inmersiva y cotizador animado, con animaciones moderadas

## Phase Details

### Phase 1: Gestión del inventario por el admin

**Goal**: La única cuenta admin gestiona el inventario completo de autos (datos, fotos, estado y destacados) desde el panel, contra la API real y con errores manejados de forma uniforme
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: ADM-01, ADM-02, ADM-03, ADM-04, ADM-05, PROD-01
**Success Criteria** (what must be TRUE):
  1. La cuenta admin existe al arrancar la app (creada por config o seed), y desde la web no se puede registrar ni obtener otra cuenta admin
  2. El admin crea, edita y elimina una publicación desde el panel, y los cambios persisten en el backend (siguen ahí al recargar)
  3. El admin sube, reordena y elimina fotos de un auto; si el archivo no es una imagen válida o supera el límite de tamaño, se rechaza con un mensaje claro
  4. El admin cambia el estado de un auto (disponible / reservado / vendido) y lo marca o desmarca como destacado desde el panel
  5. Con un token vencido o malformado, la API responde 401 (no 500) con un error de formato uniforme, y el panel lleva al admin al login en vez de romperse

**Plans**: 7/7 plans complete

Plans:
**Wave 1**
- [x] 01-01-PLAN.md — Tracer: listado admin con todos los estados (GET /api/admin/publicaciones) + 401/403 JSON y sesión vencida en el front (ola 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 01-02-PLAN.md — Cuenta admin y agencia inicial sembradas al arrancar desde ADMIN_* (falla en perfil prod) (ola 2)
- [x] 01-03-PLAN.md — Varias agencias: ABM completo con baja segura (400 si tiene autos), selector en el form y errores del backend visibles (ola 2)
- [x] 01-04-PLAN.md — TDD: formato uniforme de errores con un único advice (+ ServicioExternoException → 502) (ola 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 01-05-PLAN.md — Destacados (PATCH /destacado) + filtro por estado y búsqueda en el listado del panel (ola 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 01-06-PLAN.md — Fotos: validación de tipo real, tamaño y tope de 10, public_id, reorden con portada (ola 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [x] 01-07-PLAN.md — Borrado seguro: publicación en cascada con aviso de consultas, foto con resecuenciado, Cloudinary después del commit (ola 5)

**UI hint**: yes
**Notes**: El panel admin del front ya usa la API real (sin mocks). Esta fase completa lo que falta: seed del admin, validación y orden de fotos, flag de destacado y manejo global de errores (`@ControllerAdvice` + filtro JWT que devuelve 401).

### Phase 2: Catálogo público real en producción

**Goal**: Cualquier visitante navega el catálogo real cargado por el admin, sin mocks, en el sitio desplegado en producción
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: CAT-01, CAT-02, CAT-03, CAT-04, PROD-02, PROD-04
**Success Criteria** (what must be TRUE):
  1. Home, Autos, Agencia y Detalle muestran los autos que cargó el admin; `catalogoMock.js` y `USE_MOCK_DATA` ya no existen en el front
  2. El visitante filtra y ordena el catálogo y lo recorre paginado, sin que se traiga todo el inventario de una vez
  3. La Home muestra los autos que el admin marcó como destacados, y cada card y cada detalle muestran el estado del auto (disponible / reservado / vendido)
  4. El back (Railway/Render) y el front (Vercel) están desplegados con perfil de producción (sin `ddl-auto: update` ni `show-sql`), y el sitio público funciona contra la API productiva
  5. El backend no arranca si falta el secret JWT o las credenciales de Cloudinary, y CORS acepta la lista de orígenes aunque tenga espacios

**Plans**: 8/8 plans complete

Plans:
**Wave 1**
- [x] 02-01-PLAN.md — Tracer: Flyway (V1/V2/V3, validate, baseline) + destacados reales en la Home de punta a punta, migración segura de una base existente (ola 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 02-02-PLAN.md — Producción lista en código: Cloudinary obligatorio, healthcheck público, admin normalizado, README y demo segura con datos nuevos (ola 2)
- [x] 02-03-PLAN.md — Datos nuevos en la API: tipo de carrocería, precio anterior/oferta, zona de la agencia y fecha de venta (ola 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 02-04-PLAN.md — API del catálogo: listado paginado de a 24 con filtros, orden y vendidos al final + facetas (ola 3)
- [x] 02-05-PLAN.md — Panel y card: tipo, precio anterior y zona en los forms; estado, oferta y zona en la card; página de créditos (ola 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 02-06-PLAN.md — Front del catálogo: /autos con estado en la URL y paginador, página de agencia y Home contra la API (ola 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [x] 02-07-PLAN.md — Detalle real (vendido con aviso y parecidos, consulta rechazada si se vendió), front sin mocks y humo completo (ola 5)

**Wave 6** *(blocked on Wave 5 completion; checkpoints humanos)*
- [x] 02-08-PLAN.md — Producción: backup y ensayo con copia, deploy de Railway y Vercel, carga de la demo (ola 6, no autónomo)

**UI hint**: yes
**Notes**: Al quitar `ddl-auto: update`, el esquema pasa a migraciones versionadas (por ejemplo, Flyway) con una línea base que incluye los cambios de la Fase 1. Desde acá, las fases siguientes agregan migraciones y se verifican también en producción.

### Phase 3: Cuentas verificadas

**Goal**: Toda persona que quiera comprar o cotizar tiene una cuenta con identidad completa (nombre, mail, teléfono, DNI), ya sea que entre con mail o con Google
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, PROD-03
**Success Criteria** (what must be TRUE):
  1. El usuario se registra con nombre, apellido, mail, contraseña, teléfono y DNI; si falta el teléfono o el DNI, el registro se rechaza
  2. El usuario inicia sesión con Google; si le falta el teléfono o el DNI, se le pide completarlos antes de poder comprar o cotizar
  3. El usuario que olvidó su contraseña recibe un mail con un link y puede definir una nueva (el servicio de mail está configurado en producción)
  4. El usuario ve y edita su perfil desde la web
  5. Un visitante sin sesión que toca "Lo quiero", "Cotizá tu usado" o "Mis mensajes" es llevado al login y, al terminar, vuelve a la página donde estaba

**Plans**: 4/15 plans executed

Plans:
**Wave 1**
- [x] 03-01-PLAN.md — Tracer: migración V5 + regla de cuenta verificada + faltantes en login y registro + aviso en el front (ola 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 03-02-PLAN.md — Normalizador de teléfono y DNI y errores uniformes (403 de cuenta no verificada, 429 de límite) (ola 2)
- [x] 03-03-PLAN.md — Sesión: cuentas sin contraseña, cierre de sesiones al cambiar la contraseña y limitador de intentos (ola 2)
- [x] 03-04-PLAN.md — Servicio de mail (Brevo por API REST, log en desarrollo), guard de arranque y variables (ola 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 03-05-PLAN.md — API del perfil: GET y PUT /api/usuarios/me para completar y editar los datos (ola 3)
- [ ] 03-06-PLAN.md — Tokens de un solo uso y notificaciones por mail (ola 3)
- [ ] 03-07-PLAN.md — Login con Google en el back: verificador del ID token y vinculación de cuentas (ola 3)
- [ ] 03-08-PLAN.md — Gate del back: la consulta y Vender tu auto exigen cuenta verificada (ola 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 03-09-PLAN.md — Registro con apellido, teléfono y DNI y mail de confirmación (ola 4)
- [ ] 03-10-PLAN.md — Endpoints de cuenta: confirmar mail, recuperar contraseña, Google, cambio de contraseña y reenvío (ola 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 03-11-PLAN.md — Front: lógica del gate, Completá tus datos y sesión rehidratada (ola 5)

**Wave 6** *(blocked on Wave 5 completion)*
- [ ] 03-12-PLAN.md — Front: registro, login con Google y pantallas de recuperación y confirmación de mail (ola 6)

**Wave 7** *(blocked on Wave 6 completion)*
- [ ] 03-13-PLAN.md — Front: perfil, Lo quiero, Mis mensajes, privacidad y gate de los botones (ola 7)

**Wave 8** *(blocked on Wave 7 completion; checkpoint humano)*
- [ ] 03-14-PLAN.md — Humo completo, README y cuentas externas de Google Cloud y Brevo (ola 8, no autónomo)

**Wave 9** *(blocked on Wave 8 completion; checkpoints humanos)*
- [ ] 03-15-PLAN.md — Producción: backup, ensayo de V5 con copia, variables, deploy y prueba con casillas reales (ola 9, no autónomo)

**UI hint**: yes
**Notes**: DNI y teléfono son datos personales (Ley 25.326): exponerlos solo al propio usuario y al admin. Los botones de Google en `LoginPage.jsx` y `RegistroPage.jsx` hoy son TODOs. La migración de la fase es V5 (V4 la ocupó "Vender tu auto"). Decisiones del plan sobre la integración posterior de "Vender tu auto": su alta también exige cuenta verificada (03-08) y la ruta `/vender` queda detrás del gate (03-13).

### Phase 4: Compra por conversación con la agencia

**Goal**: Un usuario verificado toca "Lo quiero" en un auto y conversa con la agencia dentro de la web; el admin atiende todas las conversaciones desde su bandeja y sabe exactamente con quién habla
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: MSG-01, MSG-03, MSG-04, MSG-05, MSG-06, MSG-07, MSG-08, MSG-09
**Success Criteria** (what must be TRUE):
  1. El usuario toca "Lo quiero" en una publicación y se abre una conversación de compra atada a ese auto (en este flujo reemplaza al link de WhatsApp)
  2. El usuario ve "Mis mensajes", con sus conversaciones y el auto asociado a cada una, e intercambia mensajes de texto con el admin
  3. El usuario y el admin ven un contador de mensajes no leídos en la web y reciben un mail cuando les llega un mensaje nuevo
  4. El admin ve en el panel una bandeja con todas las conversaciones, la filtra por tipo, estado y no leídas, y puede cerrar o reabrir cada conversación
  5. Desde una conversación, el admin abre la ficha del usuario (nombre, teléfono, DNI, mail) con su historial de conversaciones

**Plans**: TBD
**UI hint**: yes
**Notes**: La entidad `Consulta` existente evoluciona hacia el modelo de conversaciones o se reemplaza por él. Ese modelo contempla desde el inicio el tipo "cotización" (lo usa la Fase 5). Los avisos por mail reutilizan el servicio de PROD-03. Mensajería tipo bandeja, sin tiempo real.

### Phase 5: Cotizador de usados

**Goal**: El usuario cotiza su auto en la web, recibe al instante un precio estimado y lo usa para venderle a la agencia o entregarlo como parte de pago; todo termina en una conversación con el admin
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: COT-01, COT-02, COT-03, COT-04, COT-05, COT-06, MSG-02, ADM-06
**Success Criteria** (what must be TRUE):
  1. El usuario completa el cotizador paso a paso (marca → modelo → año → versión → km → estado → fotos) y al final ve un rango de precio estimado obtenido del proveedor externo
  2. Si el proveedor de precios falla, el usuario igual puede enviar la cotización, y el admin la recibe para valuarla manualmente
  3. El usuario elige venta directa o parte de pago; tanto "Aceptar" como "Consultar otro precio" abren una conversación de cotización con el admin
  4. El usuario ve el historial de sus cotizaciones y, al tocar "Lo quiero" en un auto, puede ofrecer una de ellas como parte de pago
  5. El admin ve en el panel las métricas del mes (cotizaciones, conversaciones abiertas, autos vendidos) y las cotizaciones de cada usuario en su ficha

**Plans**: TBD
**UI hint**: yes
**Notes**: RESEARCH NEEDED. La investigación del proyecto se omitió, así que la elección del proveedor de precios (COT-02/COT-06) se investiga al inicio de esta fase. Opciones: InfoAuto, ACARA, la API de Mercado Libre con publicaciones comparables u otras. Hay que evaluar costo, acceso y cobertura del mercado argentino. El proveedor va detrás de una interfaz intercambiable con fallback a valuación manual. Las fotos de la cotización van a Cloudinary con la misma validación de ADM-03.

### Phase 6: Experiencia visual pulida

**Goal**: El sitio se siente moderno y familiar (estilo Kavak / Mercado Libre), con animaciones medidas que suman sin molestar
**Mode:** mvp
**Depends on**: Phase 5
**Requirements**: UX-01, UX-02, UX-03, UX-04, UX-05
**Success Criteria** (what must be TRUE):
  1. Cards, botones y formularios dan feedback visible al pasar el mouse y al hacer click, y las cargas muestran skeletons en vez de pantallas vacías
  2. Las secciones aparecen suavemente al scrollear, y el paso entre páginas tiene transiciones suaves
  3. En el detalle de un auto, la galería permite swipe, zoom y pantalla completa, en mobile y en desktop
  4. El cotizador muestra una barra de progreso animada entre pasos y revela el precio con una animación
  5. Con `prefers-reduced-motion` activado, las animaciones se reducen o se desactivan sin perder funcionalidad

**Plans**: TBD
**UI hint**: yes
**Notes**: Un solo sistema de motion para todo el sitio, con animaciones moderadas por pedido explícito. UX-01, UX-02 y UX-03 podrían adelantarse sobre las páginas ya existentes; UX-04 necesita el cotizador de la Fase 5.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Gestión del inventario por el admin | 7/7 | Complete    | 2026-10-03 |
| 2. Catálogo público real en producción | 8/8 | Complete    | 2026-10-05 |
| 3. Cuentas verificadas | 4/15 | In Progress|  |
| 4. Compra por conversación con la agencia | 0/TBD | Not started | - |
| 5. Cotizador de usados | 0/TBD | Not started | - |
| 6. Experiencia visual pulida | 0/TBD | Not started | - |
